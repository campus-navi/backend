package com.campusnavi.backend.official.post.repository;

import com.campusnavi.backend.global.config.JpaConfig;
import com.campusnavi.backend.global.config.QueryDslConfig;
import com.campusnavi.backend.member.entity.Member;
import com.campusnavi.backend.official.post.dto.OfficialPostRecommendCandidateRaw;
import com.campusnavi.backend.official.post.dto.OfficialPostScopeCondition;
import com.campusnavi.backend.official.post.dto.OfficialPostStatsRaw;
import com.campusnavi.backend.official.post.entity.ApplyMethodType;
import com.campusnavi.backend.official.post.entity.OfficialPost;
import com.campusnavi.backend.official.post.entity.OfficialPostAiMeta;
import com.campusnavi.backend.official.post.entity.OfficialPostView;
import com.campusnavi.backend.official.post.recommend.config.RecommendProperties;
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
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import({JpaConfig.class, QueryDslConfig.class, OfficialPostQueryRepository.class})
@EnableConfigurationProperties(RecommendProperties.class)
class OfficialPostQueryRepositoryTest extends PostgresSliceTestSupport {

    @Autowired
    private OfficialPostQueryRepository officialPostQueryRepository;

    @Autowired
    private EntityManager em;

    private University university;
    private Campus campus;
    private Department requesterDept;
    private Department otherDept;
    private OfficialSource source;
    private Tag recommendableTag;

    @BeforeEach
    void setUp() {
        university = persist(University.create("테스트대학교"));
        campus = persist(Campus.create(university, "본캠퍼스", "TEST_MAIN", "test.ac.kr"));
        requesterDept = persist(Department.create(campus, "컴퓨터공학과"));
        otherDept = persist(Department.create(campus, "수학과"));
        source = persist(OfficialSource.create(
                university.getId(), campus.getId(), null, null,
                SourceType.CRAWL, "TEST_PARSER", "테스트소스", "http://test.ac.kr/notice"));
        recommendableTag = persist(Tag.create("STUDY", "스터디", true));
        em.flush();
    }

    @Nested
    @DisplayName("findStatsByPostIds")
    class FindStatsByPostIds {

        @Test
        @DisplayName("같은 학과 단일 조회자가 있으면 cappedViewSum/sameAdmission/sameGrade/viewerCount가 정상 집계된다")
        void success() {
            // given
            OfficialPost post = persistRecommendablePost(LocalDate.now());
            Member viewer = persistActiveMember(2022, 3, requesterDept);
            persistView(viewer.getId(), post.getId(), 5);

            // when
            Map<Long, OfficialPostStatsRaw> stats = byPostId(officialPostQueryRepository.findStatsByPostIds(
                    List.of(post.getId()), 2022, 3, List.of(requesterDept.getId())));

            // then
            assertThat(stats).containsKey(post.getId());
            OfficialPostStatsRaw raw = stats.get(post.getId());
            assertThat(raw.cappedViewSum()).isEqualTo(5L);
            assertThat(raw.sameAdmissionCount()).isEqualTo(1L);
            assertThat(raw.sameGradeCount()).isEqualTo(1L);
            assertThat(raw.viewerCount()).isEqualTo(1L);
        }

        @Test
        @DisplayName("view_count가 viewCap을 초과하면 LEAST로 cap된 값이 합산된다")
        void viewCap() {
            // given
            OfficialPost post = persistRecommendablePost(LocalDate.now());
            Member viewer1 = persistActiveMember(2022, 3, requesterDept);
            Member viewer2 = persistActiveMember(2022, 3, requesterDept);
            persistView(viewer1.getId(), post.getId(), 100);
            persistView(viewer2.getId(), post.getId(), 20);

            // when
            Map<Long, OfficialPostStatsRaw> stats = byPostId(officialPostQueryRepository.findStatsByPostIds(
                    List.of(post.getId()), 2022, 3, List.of(requesterDept.getId())));

            // then
            assertThat(stats.get(post.getId()).cappedViewSum()).isEqualTo(50L);
            assertThat(stats.get(post.getId()).viewerCount()).isEqualTo(2L);
        }

        @Test
        @DisplayName("요청자 학과에 속하지 않은 회원의 조회는 EXISTS에서 제외된다")
        void otherDept() {
            // given
            OfficialPost post = persistRecommendablePost(LocalDate.now());
            Member sameDeptViewer = persistActiveMember(2022, 3, requesterDept);
            Member otherDeptViewer = persistActiveMember(2022, 3, otherDept);
            persistView(sameDeptViewer.getId(), post.getId(), 5);
            persistView(otherDeptViewer.getId(), post.getId(), 100);

            // when
            Map<Long, OfficialPostStatsRaw> stats = byPostId(officialPostQueryRepository.findStatsByPostIds(
                    List.of(post.getId()), 2022, 3, List.of(requesterDept.getId())));

            // then
            assertThat(stats.get(post.getId()).viewerCount()).isEqualTo(1L);
            assertThat(stats.get(post.getId()).cappedViewSum()).isEqualTo(5L);
        }

        @Test
        @DisplayName("이중 전공 회원이 요청자 학과 중 여러 개에 속해도 중복 카운트되지 않는다")
        void doubleMajor() {
            // given
            OfficialPost post = persistRecommendablePost(LocalDate.now());
            Member doubleMajor = persistActiveMember(2022, 3, requesterDept);
            doubleMajor.addDepartment(otherDept);
            em.persist(doubleMajor);
            em.flush();

            persistView(doubleMajor.getId(), post.getId(), 10);

            // when
            Map<Long, OfficialPostStatsRaw> stats = byPostId(officialPostQueryRepository.findStatsByPostIds(
                    List.of(post.getId()), 2022, 3,
                    List.of(requesterDept.getId(), otherDept.getId())));

            // then
            assertThat(stats.get(post.getId()).cappedViewSum()).isEqualTo(10L);
            assertThat(stats.get(post.getId()).viewerCount()).isEqualTo(1L);
            assertThat(stats.get(post.getId()).sameAdmissionCount()).isEqualTo(1L);
            assertThat(stats.get(post.getId()).sameGradeCount()).isEqualTo(1L);
        }

        @Test
        @DisplayName("WITHDRAWN 회원의 조회는 ACTIVE 조건에서 제외된다")
        void withdrawn() {
            // given
            OfficialPost post = persistRecommendablePost(LocalDate.now());
            Member activeViewer = persistActiveMember(2022, 3, requesterDept);
            Member withdrawnViewer = persistActiveMember(2022, 3, requesterDept);
            withdrawnViewer.withdraw();
            em.persist(withdrawnViewer);
            em.flush();

            persistView(activeViewer.getId(), post.getId(), 5);
            persistView(withdrawnViewer.getId(), post.getId(), 100);

            // when
            Map<Long, OfficialPostStatsRaw> stats = byPostId(officialPostQueryRepository.findStatsByPostIds(
                    List.of(post.getId()), 2022, 3, List.of(requesterDept.getId())));

            // then
            assertThat(stats.get(post.getId()).viewerCount()).isEqualTo(1L);
            assertThat(stats.get(post.getId()).cappedViewSum()).isEqualTo(5L);
        }

        @Test
        @DisplayName("빈 postIds나 빈 requesterDeptIds면 SQL 미실행으로 빈 리스트를 반환한다")
        void emptyInput() {
            // when
            List<OfficialPostStatsRaw> emptyPostIds =
                    officialPostQueryRepository.findStatsByPostIds(
                            List.of(), 2022, 3, List.of(requesterDept.getId()));

            // then
            assertThat(emptyPostIds).isEmpty();

            // when
            List<OfficialPostStatsRaw> emptyDeptIds =
                    officialPostQueryRepository.findStatsByPostIds(
                            List.of(1L), 2022, 3, List.of());

            // then
            assertThat(emptyDeptIds).isEmpty();
        }
    }

    @Nested
    @DisplayName("findRecommendCandidates")
    class FindRecommendCandidates {

        @Test
        @DisplayName("tag.isRecommendable=false인 글은 후보에서 제외된다")
        void notRecommendable() {
            // given
            Tag nonRecommendableTag = persist(Tag.create("NOTICE", "공지", false));
            OfficialPost recommendablePost = persistPostWithTag(recommendableTag, LocalDate.now(), null);
            OfficialPost nonRecommendablePost = persistPostWithTag(nonRecommendableTag, LocalDate.now(), null);

            // when
            List<OfficialPostRecommendCandidateRaw> result =
                    officialPostQueryRepository.findRecommendCandidates(condition());

            // then
            assertThat(result).extracting(OfficialPostRecommendCandidateRaw::postId)
                    .contains(recommendablePost.getId())
                    .doesNotContain(nonRecommendablePost.getId());
        }

        @Test
        @DisplayName("publishedAt이 신선도(today - freshnessDays) 이전인 글은 제외된다")
        void stale() {
            // given (freshnessDays=14)
            LocalDate today = LocalDate.now();
            OfficialPost freshPost = persistPostWithTag(recommendableTag, today.minusDays(10), null);
            OfficialPost stalePost = persistPostWithTag(recommendableTag, today.minusDays(20), null);

            // when
            List<OfficialPostRecommendCandidateRaw> result =
                    officialPostQueryRepository.findRecommendCandidates(condition());

            // then
            assertThat(result).extracting(OfficialPostRecommendCandidateRaw::postId)
                    .contains(freshPost.getId())
                    .doesNotContain(stalePost.getId());
        }

        @Test
        @DisplayName("aiMeta.endDate가 today 이전이면 마감으로 간주되어 제외된다 (null이면 포함)")
        void expired() {
            // given
            LocalDate today = LocalDate.now();
            OfficialPost nullEndDatePost = persistPostWithTag(recommendableTag, today, null);
            OfficialPost futureEndDatePost = persistPostWithTag(recommendableTag, today, today.plusDays(3));
            OfficialPost expiredPost = persistPostWithTag(recommendableTag, today, today.minusDays(2));

            // when
            List<OfficialPostRecommendCandidateRaw> result =
                    officialPostQueryRepository.findRecommendCandidates(condition());

            // then
            assertThat(result).extracting(OfficialPostRecommendCandidateRaw::postId)
                    .contains(nullEndDatePost.getId(), futureEndDatePost.getId())
                    .doesNotContain(expiredPost.getId());
        }
    }

    private <T> T persist(T entity) {
        em.persist(entity);
        return entity;
    }

    private Map<Long, OfficialPostStatsRaw> byPostId(List<OfficialPostStatsRaw> list) {
        return list.stream().collect(Collectors.toMap(OfficialPostStatsRaw::postId, s -> s));
    }

    private OfficialPostScopeCondition condition() {
        return new OfficialPostScopeCondition(
                university.getId(), campus.getId(),
                List.of(), List.of(requesterDept.getId()));
    }

    private Member persistActiveMember(int admissionYear, int grade, Department department) {
        Member member = Member.join(
                "u" + uniqueSuffix() + "@test.ac.kr",
                "user" + uniqueSuffix(),
                "password",
                "nick" + uniqueSuffix(),
                university.getId(), campus, admissionYear, grade);
        member.addDepartment(department);
        em.persist(member);
        em.flush();
        return member;
    }

    private void persistView(Long memberId, Long postId, int viewCount) {
        try {
            Constructor<OfficialPostView> ctor = OfficialPostView.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            OfficialPostView view = ctor.newInstance();
            ReflectionTestUtils.setField(view, "memberId", memberId);
            ReflectionTestUtils.setField(view, "postId", postId);
            ReflectionTestUtils.setField(view, "viewCount", viewCount);
            ReflectionTestUtils.setField(view, "firstViewedAt", LocalDateTime.now());
            ReflectionTestUtils.setField(view, "lastViewedAt", LocalDateTime.now());
            em.persist(view);
            em.flush();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private OfficialPost persistRecommendablePost(LocalDate publishedAt) {
        return persistPostWithTag(recommendableTag, publishedAt, null);
    }

    private OfficialPost persistPostWithTag(Tag tag, LocalDate publishedAt, LocalDate endDate) {
        OfficialPost post = OfficialPost.create(
                source, "orig-" + uniqueSuffix(), "title", "publisher",
                "structured", "html", "http://test.ac.kr/post",
                publishedAt, LocalDateTime.now());
        ReflectionTestUtils.setField(post, "universityId", university.getId());
        ReflectionTestUtils.setField(post, "campusId", campus.getId());
        ReflectionTestUtils.setField(post, "collegeId", null);
        ReflectionTestUtils.setField(post, "departmentId", null);
        em.persist(post);

        OfficialPostAiMeta meta = OfficialPostAiMeta.pending(post);
        meta.processCompleted(
                "summary", null, null, tag, null,
                null, null, null, null, endDate, null,
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
