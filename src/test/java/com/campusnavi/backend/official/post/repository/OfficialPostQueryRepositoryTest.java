package com.campusnavi.backend.official.post.repository;

import com.campusnavi.backend.global.config.JpaConfig;
import com.campusnavi.backend.global.config.QueryDslConfig;
import com.campusnavi.backend.official.post.dto.OfficialPostCardRaw;
import com.campusnavi.backend.official.post.dto.OfficialPostScopeCondition;
import com.campusnavi.backend.official.post.entity.ApplyMethodType;
import com.campusnavi.backend.official.post.entity.OfficialPost;
import com.campusnavi.backend.official.post.entity.OfficialPostAiMeta;
import com.campusnavi.backend.official.source.entity.OfficialSource;
import com.campusnavi.backend.official.source.entity.SourceType;
import com.campusnavi.backend.support.PostgresSliceTestSupport;
import com.campusnavi.backend.tag.entity.Tag;
import com.campusnavi.backend.university.entity.Campus;
import com.campusnavi.backend.university.entity.Department;
import com.campusnavi.backend.university.entity.University;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import({JpaConfig.class, QueryDslConfig.class, OfficialPostQueryRepository.class})
class OfficialPostQueryRepositoryTest extends PostgresSliceTestSupport {

    @Autowired
    private OfficialPostQueryRepository officialPostQueryRepository;

    @Autowired
    private EntityManager em;

    private University university;
    private Campus campus;
    private Department requesterDept;
    private OfficialSource source;
    private Tag recommendableTag;

    @BeforeEach
    void setUp() {
        university = persist(University.create("테스트대학교"));
        campus = persist(Campus.create(university, "본캠퍼스", "TEST_MAIN", "test.ac.kr"));
        requesterDept = persist(Department.create(campus, "컴퓨터공학과"));
        source = persist(OfficialSource.create(
                university.getId(), campus.getId(), null, null,
                SourceType.CRAWL, "TEST_PARSER", "테스트소스", "http://test.ac.kr/notice"));
        recommendableTag = persist(Tag.create("STUDY", "스터디", true));
        em.flush();
    }

    @Nested
    @DisplayName("findRecentPosts")
    class FindRecentPosts {

        @Test
        @DisplayName("crawledAt이 slotAt 이후인 글은 제외된다")
        void crawledAtCutoff() {
            // given
            LocalDateTime slotAt = LocalDateTime.of(2026, 5, 12, 9, 0);
            OfficialPost beforeSlot = persistPostWithCrawledAt(
                    LocalDate.of(2026, 5, 12), slotAt.minusMinutes(10));
            OfficialPost afterSlot = persistPostWithCrawledAt(
                    LocalDate.of(2026, 5, 12), slotAt.plusMinutes(10));

            // when
            List<OfficialPostCardRaw> result =
                    officialPostQueryRepository.findRecentPosts(condition(), slotAt);

            // then
            assertThat(result).extracting(OfficialPostCardRaw::postId)
                    .contains(beforeSlot.getId())
                    .doesNotContain(afterSlot.getId());
        }

        @Test
        @DisplayName("publishedAt DESC 정렬, 최대 9개로 제한된다")
        void orderAndLimit() {
            // given
            LocalDate anchor = LocalDate.of(2026, 5, 12);
            LocalDateTime crawledAt = anchor.atTime(12, 0);
            for (int i = 0; i < 12; i++) {
                persistPostWithCrawledAt(anchor.minusDays(i), crawledAt);
            }

            // when
            List<OfficialPostCardRaw> result = officialPostQueryRepository.findRecentPosts(
                    condition(), anchor.plusDays(1).atStartOfDay());

            // then
            assertThat(result).hasSize(9);
            assertThat(result)
                    .extracting(OfficialPostCardRaw::publishedAt)
                    .isSortedAccordingTo((a, b) -> b.compareTo(a));
        }
    }

    private <T> T persist(T entity) {
        em.persist(entity);
        return entity;
    }

    private OfficialPostScopeCondition condition() {
        return new OfficialPostScopeCondition(
                university.getId(), campus.getId(),
                List.of(), List.of(requesterDept.getId()));
    }

    private OfficialPost persistPostWithCrawledAt(LocalDate publishedAt, LocalDateTime crawledAt) {
        OfficialPost post = OfficialPost.create(
                source, "orig-" + uniqueSuffix(), "title", "publisher",
                "structured", "html", "http://test.ac.kr/post",
                publishedAt, crawledAt);
        ReflectionTestUtils.setField(post, "universityId", university.getId());
        ReflectionTestUtils.setField(post, "campusId", campus.getId());
        ReflectionTestUtils.setField(post, "collegeId", null);
        ReflectionTestUtils.setField(post, "departmentId", null);
        em.persist(post);

        OfficialPostAiMeta meta = OfficialPostAiMeta.pending(post);
        meta.processCompleted(
                "summary", null, null, recommendableTag, null,
                null, null, null, null, null, null,
                null, ApplyMethodType.LINK, null, null, false);
        em.persist(meta);
        em.flush();
        return post;
    }

    private long suffixCounter = 0;
    private String uniqueSuffix() {
        return String.valueOf(++suffixCounter);
    }
}
