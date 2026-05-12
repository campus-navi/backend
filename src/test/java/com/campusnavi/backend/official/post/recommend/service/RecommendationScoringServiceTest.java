package com.campusnavi.backend.official.post.recommend.service;

import com.campusnavi.backend.official.post.dto.OfficialPostRecommendCandidateRaw;
import com.campusnavi.backend.official.post.dto.OfficialPostStatsRaw;
import com.campusnavi.backend.official.post.recommend.config.RecommendProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

class RecommendationScoringServiceTest {

    private static final double DELTA = 1e-9;

    private final RecommendProperties defaultProperties =
            new RecommendProperties(0.30, 0.30, 0.40, 0.4, 0.6, 30, 200, 5, 14);

    private final RecommendationScoringService service =
            new RecommendationScoringService(defaultProperties);

    @Nested
    @DisplayName("computeS1")
    class ComputeS1 {

        @Test
        @DisplayName("정상 케이스에서 cappedViewSum/deptMemberCount/viewCap 으로 정규화된다")
        void normalize() {
            // given
            OfficialPostStatsRaw stats = stats(1L, 150L, 0L, 0L, 0L);

            // when
            double s1 = service.computeS1(stats, 10L);

            // then
            assertThat(s1).isCloseTo(0.5, offset(DELTA));
        }

        @Test
        @DisplayName("deptMemberCount가 0이면 0.0을 반환한다")
        void noDeptMember() {
            // when
            double s1 = service.computeS1(stats(1L, 9999L, 0L, 0L, 0L), 0L);

            // then
            assertThat(s1).isEqualTo(0.0);
        }

        @Test
        @DisplayName("viewCap이 0 이하면 0.0을 반환한다")
        void invalidViewCap() {
            // given
            RecommendProperties zeroCap =
                    new RecommendProperties(0.30, 0.30, 0.40, 0.4, 0.6, 0, 200, 5, 14);
            RecommendationScoringService zeroCapService = new RecommendationScoringService(zeroCap);

            // when
            double s1 = zeroCapService.computeS1(stats(1L, 100L, 0L, 0L, 0L), 10L);

            // then
            assertThat(s1).isEqualTo(0.0);
        }

        @Test
        @DisplayName("정규화 결과가 1.0을 초과하면 clamp되어 1.0을 반환한다")
        void clamp() {
            // when
            double s1 = service.computeS1(stats(1L, 999_999L, 0L, 0L, 0L), 10L);

            // then
            assertThat(s1).isEqualTo(1.0);
        }
    }

    @Nested
    @DisplayName("computeS2")
    class ComputeS2 {

        @Test
        @DisplayName("정상 케이스에서 w2Admission/w2Grade 가중치가 적용된다")
        void weighted() {
            // given - viewerCount=10 중 학번 일치 4, 학년 일치 7
            OfficialPostStatsRaw stats = stats(1L, 0L, 4L, 7L, 10L);

            // when
            double s2 = service.computeS2(stats);

            // then - 0.4 * 0.4 + 0.6 * 0.7 = 0.58
            assertThat(s2).isCloseTo(0.58, offset(DELTA));
        }

        @Test
        @DisplayName("viewerCount가 0이면 0.0을 반환한다")
        void noViewer() {
            // when
            double s2 = service.computeS2(stats(1L, 0L, 5L, 5L, 0L));

            // then
            assertThat(s2).isEqualTo(0.0);
        }

        @Test
        @DisplayName("가중치를 변경하면 결과가 그에 맞게 계산된다")
        void customWeights() {
            // given - admission=0.6, grade=0.4 로 뒤집기
            RecommendProperties reversed =
                    new RecommendProperties(0.30, 0.30, 0.40, 0.6, 0.4, 30, 200, 5, 14);
            RecommendationScoringService reversedService = new RecommendationScoringService(reversed);
            OfficialPostStatsRaw stats = stats(1L, 0L, 4L, 7L, 10L);

            // when
            double s2 = reversedService.computeS2(stats);

            // then - 0.6 * 0.4 + 0.4 * 0.7 = 0.52
            assertThat(s2).isCloseTo(0.52, offset(DELTA));
        }
    }

    @Nested
    @DisplayName("computeS3")
    class ComputeS3 {

        @Test
        @DisplayName("postTagId가 관심 태그에 있으면 1.0을 반환한다")
        void match() {
            // when
            double s3 = service.computeS3(100L, Set.of(100L, 200L));

            // then
            assertThat(s3).isEqualTo(1.0);
        }

        @Test
        @DisplayName("postTagId가 관심 태그에 없으면 0.0을 반환한다")
        void mismatch() {
            // when
            double s3 = service.computeS3(100L, Set.of(200L));

            // then
            assertThat(s3).isEqualTo(0.0);
        }

        @Test
        @DisplayName("관심 태그가 비어있으면 0.0을 반환한다")
        void noInterest() {
            // when
            double s3 = service.computeS3(100L, Set.of());

            // then
            assertThat(s3).isEqualTo(0.0);
        }

        @Test
        @DisplayName("postTagId가 null이면 0.0을 반환한다")
        void nullTagId() {
            // when
            double s3 = service.computeS3(null, Set.of(100L));

            // then
            assertThat(s3).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("rank — 정렬")
    class Sorting {

        @Test
        @DisplayName("score가 다른 두 후보 중 높은 score가 앞에 정렬된다")
        void scoreDesc() {
            // given - 1L은 S3=1.0 (score=0.40), 2L은 S3=0.0 (score=0)
            Long matchTag = 100L;
            List<OfficialPostRecommendCandidateRaw> candidates = List.of(
                    candidate(1L, matchTag, LocalDate.of(2026, 5, 1)),
                    candidate(2L, null, LocalDate.of(2026, 5, 1))
            );

            // when
            List<OfficialPostRecommendCandidateRaw> ranked =
                    service.rank(candidates, Map.of(), Set.of(matchTag), 10L);

            // then
            assertThat(ranked).extracting(OfficialPostRecommendCandidateRaw::postId)
                    .containsExactly(1L, 2L);
        }

        @Test
        @DisplayName("score 동점이면 publishedAt DESC로 정렬된다")
        void scoreTie() {
            // given
            List<OfficialPostRecommendCandidateRaw> candidates = List.of(
                    candidate(1L, null, LocalDate.of(2026, 5, 1)),
                    candidate(2L, null, LocalDate.of(2026, 5, 10))
            );

            // when
            List<OfficialPostRecommendCandidateRaw> ranked =
                    service.rank(candidates, Map.of(), Set.of(), 10L);

            // then
            assertThat(ranked).extracting(OfficialPostRecommendCandidateRaw::postId)
                    .containsExactly(2L, 1L);
        }

        @Test
        @DisplayName("score와 publishedAt이 동점이면 postId DESC로 정렬된다")
        void dateTie() {
            // given
            LocalDate sameDate = LocalDate.of(2026, 5, 1);
            List<OfficialPostRecommendCandidateRaw> candidates = List.of(
                    candidate(1L, null, sameDate),
                    candidate(3L, null, sameDate),
                    candidate(2L, null, sameDate)
            );

            // when
            List<OfficialPostRecommendCandidateRaw> ranked =
                    service.rank(candidates, Map.of(), Set.of(), 10L);

            // then
            assertThat(ranked).extracting(OfficialPostRecommendCandidateRaw::postId)
                    .containsExactly(3L, 2L, 1L);
        }

        @Test
        @DisplayName("publishedAt이 null인 후보는 nullsLast로 가장 뒤에 온다")
        void nullDate() {
            // given
            List<OfficialPostRecommendCandidateRaw> candidates = List.of(
                    candidate(1L, null, null),
                    candidate(2L, null, LocalDate.of(2026, 5, 1)),
                    candidate(3L, null, LocalDate.of(2026, 4, 1))
            );

            // when
            List<OfficialPostRecommendCandidateRaw> ranked =
                    service.rank(candidates, Map.of(), Set.of(), 10L);

            // then
            assertThat(ranked).extracting(OfficialPostRecommendCandidateRaw::postId)
                    .containsExactly(2L, 3L, 1L);
        }
    }

    @Nested
    @DisplayName("rank — 반환 결과")
    class Result {

        @Test
        @DisplayName("resultLimit 개수만큼만 반환한다")
        void limited() {
            // given
            List<OfficialPostRecommendCandidateRaw> candidates =
                    LongStream.rangeClosed(1, 8)
                            .mapToObj(i -> candidate(i, null, LocalDate.of(2026, 5, (int) i)))
                            .toList();

            // when
            List<OfficialPostRecommendCandidateRaw> ranked =
                    service.rank(candidates, Map.of(), Set.of(), 10L);

            // then
            assertThat(ranked).hasSize(5);
        }

        @Test
        @DisplayName("빈 candidates는 빈 리스트를 반환한다")
        void empty() {
            // when
            List<OfficialPostRecommendCandidateRaw> ranked =
                    service.rank(List.of(), Map.of(), Set.of(), 10L);

            // then
            assertThat(ranked).isEmpty();
        }

        @Test
        @DisplayName("원본 candidate 객체가 결과에 그대로 보존된다")
        void preserved() {
            // given
            OfficialPostRecommendCandidateRaw original =
                    new OfficialPostRecommendCandidateRaw(
                            1L, "원본 제목", 100L, "원본 태그",
                            "원본 요약", "원본 키", LocalDate.of(2026, 5, 1));

            // when
            List<OfficialPostRecommendCandidateRaw> ranked =
                    service.rank(List.of(original), Map.of(), Set.of(), 10L);

            // then
            assertThat(ranked).hasSize(1);
            assertThat(ranked.get(0)).isSameAs(original);
        }
    }

    private OfficialPostRecommendCandidateRaw candidate(long postId, Long tagId, LocalDate publishedAt) {
        return new OfficialPostRecommendCandidateRaw(
                postId, "title-" + postId, tagId,
                tagId == null ? null : "tag-" + tagId,
                "summary-" + postId, "key-" + postId, publishedAt);
    }

    private OfficialPostStatsRaw stats(long postId, long cappedViewSum,
                                       long sameAdmission, long sameGrade, long viewerCount) {
        return new OfficialPostStatsRaw(postId, cappedViewSum, sameAdmission, sameGrade, viewerCount);
    }
}
