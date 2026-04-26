package com.campusnavi.backend.infra.email;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;

@Configuration
@Profile("prod")
@RequiredArgsConstructor
public class SesConfig {

    private final EmailProperties emailProperties;

    @Bean
    public SesClient sesClient(){
        return SesClient.builder()
                .region(Region.of(emailProperties.awsRegion()))
                .build();
    }
}
