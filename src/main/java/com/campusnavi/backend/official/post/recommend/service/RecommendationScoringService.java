package com.campusnavi.backend.official.post.recommend.service;

import com.campusnavi.backend.official.post.dto.OfficialPostRecommendCandidateRaw;
import com.campusnavi.backend.official.post.dto.OfficialPostStatsRaw;
import com.campusnavi.backend.official.post.recommend.config.RecommendProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RecommendationScoringService {

    private final RecommendProperties properties;

    public List<OfficialPostRecommendCandidateRaw> rank(
            List<OfficialPostRecommendCandidateRaw> candidates,
            Map<Long, OfficialPostStatsRaw> statsByPostId,
            Set<Long> interestTagIds,
            long deptMemberCount
    ) {
        record Scored(OfficialPostRecommendCandidateRaw c, double score) {}

        Comparator<Scored> byScoreThenLatestThenId =
                Comparator.comparingDouble(Scored::score).reversed()
                        .thenComparing(s -> s.c().publishedAt(), Comparator.reverseOrder())
                        .thenComparing(s -> s.c().postId(), Comparator.reverseOrder());

        return candidates.stream()
                .map(c -> {
                    OfficialPostStatsRaw stats = statsByPostId
                            .getOrDefault(c.postId(), OfficialPostStatsRaw.empty(c.postId()));
                    double s1 = computeS1(stats, deptMemberCount);
                    double s2 = computeS2(stats);
                    double s3 = computeS3(c.tagId(), interestTagIds);
                    double score = properties.w1() * s1
                            + properties.w2() * s2
                            + properties.w3() * s3;
                    return new Scored(c, score);
                })
                .sorted(byScoreThenLatestThenId)
                .limit(properties.resultLimit())
                .map(Scored::c)
                .toList();
    }

    double computeS1(OfficialPostStatsRaw stats, long deptMemberCount) {
        if (deptMemberCount == 0 || properties.viewCap() <= 0) return 0.0;
        double avgView = stats.cappedViewSum() / (double) deptMemberCount;
        return clamp(avgView / properties.viewCap());
    }

    double computeS2(OfficialPostStatsRaw stats) {
        if (stats.viewerCount() == 0) return 0.0;
        double sa = stats.sameAdmissionCount() / (double) stats.viewerCount();
        double sb = stats.sameGradeCount() / (double) stats.viewerCount();
        return properties.w2Admission() * sa + properties.w2Grade() * sb;
    }

    double computeS3(Long postTagId, Set<Long> interestTagIds) {
        if (interestTagIds.isEmpty()) return 0.0;
        return (postTagId != null && interestTagIds.contains(postTagId)) ? 1.0 : 0.0;
    }

    private double clamp(double v) {
        if (v < 0.0) return 0.0;
        if (v > 1.0) return 1.0;
        return v;
    }
}
