package com.campusnavi.backend.official.crawler.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.crawl")
public record CrawlProperties(
        boolean enabled,
        Retry retry
) {

    public CrawlProperties {
        if (retry == null) retry = new Retry(3);
    }

    public record Retry(int maxCount) {
    }
}
