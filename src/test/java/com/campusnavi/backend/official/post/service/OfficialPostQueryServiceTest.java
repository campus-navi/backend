package com.campusnavi.backend.official.post.service;

import com.campusnavi.backend.global.common.AuthContext;
import com.campusnavi.backend.global.exception.BusinessException;
import com.campusnavi.backend.global.exception.ErrorCode;
import com.campusnavi.backend.infra.storage.S3StorageService;
import com.campusnavi.backend.member.dto.MemberScope;
import com.campusnavi.backend.member.entity.Member;
import com.campusnavi.backend.member.entity.MemberRole;
import com.campusnavi.backend.member.repository.MemberInterestRepository;
import com.campusnavi.backend.member.repository.MemberQueryRepository;
import com.campusnavi.backend.member.repository.MemberRepository;
import com.campusnavi.backend.official.post.dto.OfficialPostCardRaw;
import com.campusnavi.backend.official.post.dto.OfficialPostCardResponse;
import com.campusnavi.backend.official.post.dto.OfficialPostRecommendCandidateRaw;
import com.campusnavi.backend.official.post.recommend.service.RecommendationScoringService;
import com.campusnavi.backend.official.post.repository.OfficialPostQueryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OfficialPostQueryServiceTest {

    @Mock
    private MemberQueryRepository memberQueryRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private OfficialPostQueryRepository officialPostQueryRepository;

    @Mock
    private S3StorageService s3StorageService;

    @Mock
    private MemberInterestRepository interestRepository;

    @Mock
    private RecommendationScoringService scoringService;

    @InjectMocks
    private OfficialPostQueryService officialPostQueryService;

    private static final Long MEMBER_ID = 1L;
    private static final Long UNIVERSITY_ID = 100L;
    private static final Long DEPT_ID = 10L;
    private final AuthContext context = new AuthContext(MEMBER_ID, UNIVERSITY_ID);

    @Nested
    @DisplayName("getRecommendPosts")
    class GetRecommendPosts {

        @Test
        @DisplayName("후보가 비어있으면 이후 SQL을 호출하지 않고 빈 리스트를 반환한다")
        void empty() {
            // given
            Member requester = mock(Member.class);
            given(requester.getRole()).willReturn(MemberRole.USER);
            given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(requester));
            MemberScope scope = new MemberScope(null, null, DEPT_ID);
            given(memberQueryRepository.findScopesByMemberId(MEMBER_ID)).willReturn(List.of(scope));
            given(officialPostQueryRepository.findRecommendCandidates(any())).willReturn(List.of());

            // when
            List<OfficialPostCardResponse> result = officialPostQueryService.getRecommendPosts(context);

            // then
            assertThat(result).isEmpty();
            verifyNoInteractions(interestRepository);
            verifyNoInteractions(scoringService);
            verify(officialPostQueryRepository, never()).findStatsByPostIds(any(), anyInt(), anyInt(), any());
            verify(memberQueryRepository, never()).countActiveMembersInDepartments(any());
        }

        @Test
        @DisplayName("ADMIN 회원은 스코어링을 우회하고 최신순 fallback을 반환한다")
        void adminFallback() {
            // given
            Member admin = mock(Member.class);
            given(admin.getRole()).willReturn(MemberRole.ADMIN);
            given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(admin));
            given(memberQueryRepository.findScopesByMemberId(MEMBER_ID)).willReturn(List.of());

            OfficialPostCardRaw raw = new OfficialPostCardRaw(
                    1L, "title-1", "tag-1", "summary-1", "key-1", LocalDate.of(2026, 5, 1));
            given(officialPostQueryRepository.findRecentPosts(any())).willReturn(List.of(raw));
            given(s3StorageService.resolveUrl("key-1")).willReturn("https://cdn.test/key-1");

            // when
            List<OfficialPostCardResponse> result = officialPostQueryService.getRecommendPosts(context);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).postId()).isEqualTo(1L);
            verify(officialPostQueryRepository, never()).findRecommendCandidates(any());
            verify(officialPostQueryRepository, never()).findStatsByPostIds(any(), anyInt(), anyInt(), any());
            verifyNoInteractions(interestRepository);
            verifyNoInteractions(scoringService);
        }

        @Test
        @DisplayName("요청자 조회에서 회원을 찾지 못하면 MEMBER_NOT_FOUND 예외가 발생한다")
        void memberNotFound() {
            // given
            given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> officialPostQueryService.getRecommendPosts(context))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);

            verify(officialPostQueryRepository, never()).findRecommendCandidates(any());
            verify(officialPostQueryRepository, never()).findStatsByPostIds(any(), anyInt(), anyInt(), any());
            verify(interestRepository, never()).findTagIdsByMemberId(any());
            verify(memberQueryRepository, never()).countActiveMembersInDepartments(any());
            verifyNoInteractions(scoringService);
        }

        @Test
        @DisplayName("정상 흐름에서 SQL이 정해진 순서로 호출되고 스코어링 결과가 응답으로 매핑된다")
        void success() {
            // given
            LocalDate publishedAt = LocalDate.of(2026, 5, 1);
            MemberScope scope = new MemberScope(null, null, DEPT_ID);
            given(memberQueryRepository.findScopesByMemberId(MEMBER_ID)).willReturn(List.of(scope));

            OfficialPostRecommendCandidateRaw candidate = candidate(1L, publishedAt);
            given(officialPostQueryRepository.findRecommendCandidates(any()))
                    .willReturn(List.of(candidate));

            Member requester = mock(Member.class);
            given(requester.getAdmissionYear()).willReturn(2022);
            given(requester.getGrade()).willReturn(3);
            given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(requester));

            given(officialPostQueryRepository.findStatsByPostIds(
                    eq(List.of(1L)), eq(2022), eq(3), eq(List.of(DEPT_ID))))
                    .willReturn(List.of());
            given(interestRepository.findTagIdsByMemberId(MEMBER_ID)).willReturn(List.of(100L));
            given(memberQueryRepository.countActiveMembersInDepartments(eq(List.of(DEPT_ID))))
                    .willReturn(10L);

            given(scoringService.rank(
                    eq(List.of(candidate)), any(), eq(Set.of(100L)), eq(10L)))
                    .willReturn(List.of(candidate));

            given(s3StorageService.resolveUrl("key-1")).willReturn("https://cdn.test/key-1");

            // when
            List<OfficialPostCardResponse> result = officialPostQueryService.getRecommendPosts(context);

            // then
            assertThat(result).hasSize(1);
            OfficialPostCardResponse response = result.get(0);
            assertThat(response.postId()).isEqualTo(1L);
            assertThat(response.title()).isEqualTo("title-1");
            assertThat(response.tagName()).isEqualTo("tag-1");
            assertThat(response.summary()).isEqualTo("summary-1");
            assertThat(response.imageUrl()).isEqualTo("https://cdn.test/key-1");
            assertThat(response.publishedAt()).isEqualTo(publishedAt);

            InOrder order = inOrder(
                    memberQueryRepository, officialPostQueryRepository,
                    memberRepository, interestRepository, scoringService);
            order.verify(memberRepository).findById(MEMBER_ID);
            order.verify(memberQueryRepository).findScopesByMemberId(MEMBER_ID);
            order.verify(officialPostQueryRepository).findRecommendCandidates(any());
            order.verify(officialPostQueryRepository).findStatsByPostIds(
                    eq(List.of(1L)), eq(2022), eq(3), eq(List.of(DEPT_ID)));
            order.verify(interestRepository).findTagIdsByMemberId(MEMBER_ID);
            order.verify(memberQueryRepository).countActiveMembersInDepartments(eq(List.of(DEPT_ID)));
            order.verify(scoringService).rank(
                    eq(List.of(candidate)), any(), eq(Set.of(100L)), eq(10L));
        }
    }

    private static OfficialPostRecommendCandidateRaw candidate(long postId) {
        return candidate(postId, LocalDate.of(2026, 5, 1));
    }

    private static OfficialPostRecommendCandidateRaw candidate(long postId, LocalDate publishedAt) {
        return new OfficialPostRecommendCandidateRaw(
                postId, "title-" + postId, 100L, "tag-" + postId,
                "summary-" + postId, "key-" + postId, publishedAt);
    }
}
