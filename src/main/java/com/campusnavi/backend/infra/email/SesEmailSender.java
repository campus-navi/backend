package com.campusnavi.backend.infra.email;

import com.campusnavi.backend.global.exception.BusinessException;
import com.campusnavi.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

import java.nio.charset.StandardCharsets;

@Component
@Profile("ses")
@RequiredArgsConstructor
public class SesEmailSender implements EmailSender{

    private final SesClient sesClient;
    private final EmailProperties emailProperties;

    @Override
    public void send(String to, String subject, String content) {
        try {
            SendEmailRequest request = SendEmailRequest.builder()
                    .source(emailProperties.from())
                    .destination(Destination.builder().toAddresses(to).build())
                    .message(Message.builder()
                            .subject(Content.builder().data(subject).charset(StandardCharsets.UTF_8.name()).build())
                            .body(Body.builder()
                                    .html(Content.builder().data(content).charset(StandardCharsets.UTF_8.name()).build())
                                    .build())
                            .build())
                    .build();
            sesClient.sendEmail(request);
        } catch (SesException e) {
            throw new BusinessException(ErrorCode.EMAIL_SEND_FAIL, e);
        }
    }
}
