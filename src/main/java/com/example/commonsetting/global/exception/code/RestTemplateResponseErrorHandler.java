package com.example.commonsetting.global.exception.code;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;

import java.io.IOException;

@Slf4j
public class RestTemplateResponseErrorHandler implements ResponseErrorHandler {

    @Override
    public boolean hasError(ClientHttpResponse httpResponse) throws IOException {
        /**
         *  getStatusCode()가 에러나서 임시로 200이 아닌건 Error로 수정해둠.
         * */
//      return (httpResponse.getStatusCode().series() == CLIENT_ERROR || httpResponse.getStatusCode().series() == SERVER_ERROR);
        return HttpStatus.OK.value() != httpResponse.getStatusCode().value();
    }

    @Override
    public void handleError(ClientHttpResponse httpResponse) throws IOException {
        log.error("check rest client, http code={}", httpResponse.getStatusCode().value());
    }
}