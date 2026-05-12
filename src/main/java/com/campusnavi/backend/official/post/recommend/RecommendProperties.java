package com.campusnavi.backend.official.post.recommend;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "recommend.feed")
public record RecommendProperties(
        double w1,
        double w2,
        double w3,
        double w2Admission,
        double w2Grade,
        int viewCap,
        int candidateLimit,
        int resultLimit,
        int freshnessDays
) {
}
