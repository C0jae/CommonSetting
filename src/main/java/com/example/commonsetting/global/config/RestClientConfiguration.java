package com.example.commonsetting.global.config;

import com.example.commonsetting.global.exception.code.RestTemplateResponseErrorHandler;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.slf4j.MDC;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.*;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StopWatch;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.Collections;

import static com.example.commonsetting.global.exception.code.CommonExternalResponseCode.API_EXCHANGE_ERROR;
import static com.example.commonsetting.global.util.ComUtils.TRACE_ID;
import static com.example.commonsetting.global.util.SessionConstants.MEMID;
import static java.time.LocalDateTime.now;
import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;

@Configuration
public class RestClientConfiguration {
    @Bean
    @Primary
    public RestTemplate restClient() throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException {
        return getRestTemplate(30L);
    }

    @Bean
    public RestTemplate restClientMin2() throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException {
        return getRestTemplate(2 * 60L);
    }

    private RestTemplate getRestTemplate(Long readTimeOut) throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException {
        // 1) 모든 SSL 인증서를 신뢰하도록 TrustManager 설정
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
            public void checkClientTrusted(X509Certificate[] certs, String authType) {}
            public void checkServerTrusted(X509Certificate[] certs, String authType) {}
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        }};

        // 2) SSLContext를 설정하여 SSL 인증을 무시
        SSLContext sslContext = SSLContexts.custom()
                .loadTrustMaterial(((chain, authType) -> true)) // 모든 인증서를 신뢰
                .build();

        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(50);            // 이전 setMaxConnTotal 대체
        connectionManager.setDefaultMaxPerRoute(50);  // 이전 setMaxConnPerRoute 대체

        // 3) 커넥션 풀 매니저 설정

        // 4) HttpClient를 구성
        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)          // 커넥션 풀 적용
                .setSSLContext(sslContext)                        // SSLContext 적용
                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE) // 호스트 이름 검증 비활성화
                .build();

        // 5) HttpComponentsClientHttpRequestFactory 설정
        HttpComponentsClientHttpRequestFactory httpRequestFactory = new HttpComponentsClientHttpRequestFactory();
        httpRequestFactory.setHttpClient(httpClient);

        // 6) RestTemplate 생성
        RestTemplate restTemplate = new RestTemplateBuilder()
                .setConnectTimeout(Duration.ofSeconds(10))                     // 연결 타임아웃 설정
                .setReadTimeout(Duration.ofSeconds(readTimeOut))               // 읽기 타임아웃 설정
                .requestFactory(() -> new BufferingClientHttpRequestFactory(httpRequestFactory)) // RequestFactory 설정
                .build();

        // 7) Interceptor 및 ErrorHandler 추가
        restTemplate.setInterceptors(Collections.singletonList(new RequestResponseLoggingInterceptor()));
        restTemplate.setErrorHandler(new RestTemplateResponseErrorHandler());

        return restTemplate;
    }

    @Slf4j
    private static class RequestResponseLoggingInterceptor implements ClientHttpRequestInterceptor {
        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
            var dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
            var stopWatch = new StopWatch();
            stopWatch.start();
            var startedAt = now().format(dateTimeFormatter);
            var apiRequest = traceRequest(request, body);
            ClientHttpResponse response = null;
            try {
                response = execution.execute(request, body);
            } catch (IOException e) {
                log.error(getStackTrace(e));
                throw new StackExchangeException(API_EXCHANGE_ERROR.getHttpStatus(), API_EXCHANGE_ERROR.getCode(), "API 통신 중 오류가 발생했습니다");
            } catch (Exception e) {
                log.error(getStackTrace(e));
                throw new StackExchangeException(API_EXCHANGE_ERROR.getHttpStatus(), API_EXCHANGE_ERROR.getCode(), "API 통신 중 오류가 발생했습니다.");
            } finally {
                stopWatch.stop();
                var apiResponse = traceResponse(response, stopWatch.getTotalTimeMillis());
                printRestLog(apiRequest, apiResponse, startedAt);
            }
            return response;
        }

        private void printRestLog(Request request, Response response, String startedAt) {
            try {
                var traceId = MDC.get(TRACE_ID);
                var callerId = getCallerId();
                response = ObjectUtils.isEmpty(response) ? new Response(null, null, "") : response;
                var restLog = new RestLog(traceId, startedAt, callerId, request, response);
                var gson = new GsonBuilder().serializeNulls().create();
                var logs = gson.toJson(restLog);
                log.info(logs);
            } catch (Exception e) {
                log.error("error");
            }
        }

        private static String getCallerId() {
            try {
                var attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
                return (String) attributes.getRequest().getSession().getAttribute(MEMID);
            } catch (Exception e) {
                return "";
            }
        }

        private Request traceRequest(HttpRequest request, byte[] body) {
            var requestBody = new String(body, StandardCharsets.UTF_8);

            String reqLog = "\n ===========================request begin===========================" +
                    String.format("%n URI         : %s", request.getURI()) +
                    String.format("%n PATH        : %s", request.getURI().getPath()) +
                    String.format("%n QUERY       : %s", request.getURI().getQuery()) +
                    String.format("%n Method      : %s", request.getMethod()) +
                    String.format("%n Headers     : %s", request.getHeaders()) +
                    String.format("%n Request body: %s", requestBody) +
                    "\n ============================request end============================";
            log.debug(reqLog);

            return new Request(request.getURI().getHost(), request.getURI().getPath(), request.getMethod().name(), requestBody, request.getURI().getQuery());
        }

        private Response traceResponse(ClientHttpResponse response, Long executionTime) throws IOException {
            StringBuilder resLog = new StringBuilder();
            if (null == response) return null;
            StringBuilder inputStringBuilder = new StringBuilder();
            try {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(response.getBody(), StandardCharsets.UTF_8));
                String line = bufferedReader.readLine();
                while (line != null) {
                    inputStringBuilder.append(line);
                    inputStringBuilder.append('\n');
                    line = bufferedReader.readLine();
                }
            } catch (IOException e) {
                log.error(getStackTrace(e));
            }

            resLog.append("\n ==========================response begin===========================");
            resLog.append(String.format("%n Status code  : %s", response.getStatusCode().value()));
            resLog.append(String.format("%n Status text  : %s", response.getStatusText()));
            resLog.append(String.format("%n Headers      : %s", response.getHeaders()));
            resLog.append(String.format("%n Response body: %s", inputStringBuilder));
            resLog.append("\n ===========================response end============================");
            log.debug(resLog.toString());

            return new Response(response.getStatusCode().value(), executionTime, String.valueOf(inputStringBuilder));
        }

        record RestLog(String traceId, String timestamp, String callerId, Request request, Response response) {}
        record Request(String host, String url, String method, String body, String queryString) {}
        record Response(Integer status, Long duration, String payload) {}
    }
}
