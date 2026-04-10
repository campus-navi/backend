package com.campusnavi.backend.global.config;

import com.campusnavi.backend.infra.email.EmailProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;

@Configuration
@Profile({"dev","prod"})
@RequiredArgsConstructor
public class AwsConfig {

    private final EmailProperties emailProperties;

    @Bean
    public SesClient sesClient(){
        return SesClient.builder()
                .region(Region.of(emailProperties.awsRegion()))
                .build();
    }
}
