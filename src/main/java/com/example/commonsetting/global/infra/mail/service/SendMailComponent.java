package com.example.commonsetting.global.infra.mail.service;

import com.example.commonsetting.global.infra.mail.dto.MailDto;
import ktcloud.gcloud.global.exception.CustomException;
import ktcloud.gcloud.infra.mail.dto.MailDto;
import ktcloud.gcloud.infra.mail.type.MailProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static java.lang.String.join;
import static javax.mail.internet.InternetAddress.parse;
import static ktcloud.gcloud.global.exception.code.CommonResponseCode.FAILED_MAILING;
import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;
import static org.hibernate.internal.util.collections.CollectionHelper.isNotEmpty;

@Slf4j
@Component
@RequiredArgsConstructor
public class SendMailComponent {
    private final JavaMailSenderImpl javaMailSender;
    private final SpringTemplateEngine springTemplateEngine;
    private final MailProperties mailProperties;
    private final MessageSource messageSource;

    public void sendMail(MailDto info) {
        var isSuccess = false;
        var subject = "";

        try {
            subject = getMailTitle(info.titleMessageCode(),info.service());

            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, false, "UTF-8"); // use multipart (true)
            mimeMessageHelper.setSubject(subject);
            mimeMessageHelper.setText(getTemplate(info.content().templateHtml(), info.content().variables()), true);
            mimeMessageHelper.setFrom(new InternetAddress(mailProperties.from().address(), mailProperties.from().name(), "UTF-8"));
            mimeMessageHelper.setTo(getAddressParse(info.toAddressList()));

            javaMailSender.send(mimeMessage);
            isSuccess = true;

        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error("send mail error-message \n{}", getStackTrace(e));
            throw new CustomException(FAILED_MAILING);
        } catch (Exception e) {
            log.error("send mail (exception) error-message \n{}", getStackTrace(e));
            throw new CustomException(FAILED_MAILING);
        } finally {
            log.info("send mail [{}] => subject: {}, to: {}", isSuccess, subject, info.toAddressList());
        }
    }

    private String getTemplate(String fileName, final Map<String, Object> variables) {
        return springTemplateEngine.process(fileName, addCommonVariable(variables));
    }

    private Context addCommonVariable(final Map<String, Object> variables) {
        Context context = new Context();
        context.setVariables(variables);
        context.setVariable("imageURL", mailProperties.imageUrl());
        context.setVariable("ts", System.currentTimeMillis());
        return context;
    }

    private InternetAddress[] getAddressParse(List<String> addressList) throws AddressException {
        if (isNotEmpty(addressList)) {
            return parse(join(",", addressList), true);
        }
        return new InternetAddress[0];
    }

    private String getMailTitle(String titleCodeName, String... values) {
        return messageSource.getMessage(titleCodeName, values, "", Locale.getDefault());
    }
}