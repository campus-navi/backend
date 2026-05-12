package com.campusnavi.backend.official.post.recommend.service;

import com.campusnavi.backend.global.common.AuthContext;
import com.campusnavi.backend.member.dto.MemberScope;
import com.campusnavi.backend.member.entity.Member;
import com.campusnavi.backend.member.repository.MemberInterestRepository;
import com.campusnavi.backend.member.repository.MemberQueryRepository;
import com.campusnavi.backend.official.post.dto.OfficialPostCardRaw;
import com.campusnavi.backend.official.post.dto.OfficialPostCardResponse;
import com.campusnavi.backend.official.post.dto.OfficialPostRecommendCandidateRaw;
import com.campusnavi.backend.official.post.recommend.entity.FeedRecommendSnapshot;
import com.campusnavi.backend.official.post.recommend.repository.FeedRecommendSnapshotRepository;
import com.campusnavi.backend.official.post.recommend.repository.RecommendQueryRepository;
import com.campusnavi.backend.official.post.service.OfficialPostCardResponseMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class RecommendQueryServiceTest {

    @Mock
    private MemberQueryRepository memberQueryRepository;

    @Mock
    private MemberInterestRepository interestRepository;

    @Mock
    private RecommendationScoringService scoringService;

    @Mock
    private RecommendQueryRepository recommendQueryRepository;

    @Mock
    private FeedRecommendSnapshotRepository snapshotRepository;

    @Mock
    private RecommendSnapshotWriter snapshotWriter;

    @Mock
    private OfficialPostCardResponseMapper cardResponseMapper;

    @InjectMocks
    private RecommendQueryService recommendQueryService;

    private static final Long MEMBER_ID = 1L;
    private static final Long UNIVERSITY_ID = 100L;
    private static final Long DEPT_ID = 10L;
    private static final AuthContext CONTEXT = new AuthContext(MEMBER_ID, UNIVERSITY_ID);

    @Nested
    @DisplayName("getRecommendPosts")
    class GetRecommendPosts {

        @Test
        @DisplayName("스냅샷 hit 시 저장된 순서대로 카드를 응답한다")
        void snapshotHit() {
            // given
            Member requester = activeMember();
            given(memberQueryRepository.findScopesByMemberId(MEMBER_ID)).willReturn(List.of());

            FeedRecommendSnapshot snapshot = FeedRecommendSnapshot.create(
                    MEMBER_ID, LocalDateTime.now(), List.of(2L, 1L));
            given(snapshotRepository.findByMemberIdAndSlotAt(eq(MEMBER_ID), any()))
                    .willReturn(Optional.of(snapshot));

            OfficialPostCardRaw card1 = card(1L);
            OfficialPostCardRaw card2 = card(2L);
            given(recommendQueryRepository.findCardsByIds(eq(List.of(2L, 1L))))
                    .willReturn(List.of(card1, card2));
            given(cardResponseMapper.toResponse(card1)).willReturn(response(1L));
            given(cardResponseMapper.toResponse(card2)).willReturn(response(2L));

            // when
            List<OfficialPostCardResponse> result = recommendQueryService.getRecommendPosts(CONTEXT, requester);

            // then
            assertThat(result).extracting(OfficialPostCardResponse::postId).containsExactly(2L, 1L);
            then(recommendQueryRepository).should(never())
                    .findRecommendCandidates(any(), any(), any());
            then(scoringService).shouldHaveNoInteractions();
            then(snapshotWriter).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("스냅샷 miss 시 후보·스코어링 후 카드를 응답한다")
        void snapshotMiss() {
            // given
            Member requester = activeMember();
            MemberScope scope = new MemberScope(null, null, DEPT_ID);
            given(memberQueryRepository.findScopesByMemberId(MEMBER_ID)).willReturn(List.of(scope));
            given(snapshotRepository.findByMemberIdAndSlotAt(eq(MEMBER_ID), any()))
                    .willReturn(Optional.empty());

            OfficialPostRecommendCandidateRaw candidate = candidate(7L);
            given(recommendQueryRepository.findRecommendCandidates(any(), any(), eq(MEMBER_ID)))
                    .willReturn(List.of(candidate));
            given(recommendQueryRepository.findStatsByPostIds(
                    eq(List.of(7L)), eq(2022), eq(3), eq(List.of(DEPT_ID))))
                    .willReturn(List.of());
            given(interestRepository.findTagIdsByMemberId(MEMBER_ID)).willReturn(List.of(100L));
            given(memberQueryRepository.countActiveMembersInDepartments(eq(List.of(DEPT_ID))))
                    .willReturn(10L);
            given(scoringService.rank(
                    eq(List.of(candidate)), any(), eq(Set.of(100L)), eq(10L)))
                    .willReturn(List.of(candidate));

            OfficialPostCardRaw card = card(7L);
            given(recommendQueryRepository.findCardsByIds(eq(List.of(7L))))
                    .willReturn(List.of(card));
            given(cardResponseMapper.toResponse(card)).willReturn(response(7L));

            // when
            List<OfficialPostCardResponse> result = recommendQueryService.getRecommendPosts(CONTEXT, requester);

            // then
            assertThat(result).extracting(OfficialPostCardResponse::postId).containsExactly(7L);
            then(snapshotWriter).should().persist(eq(MEMBER_ID), any(), eq(List.of(7L)));
        }

        @Test
        @DisplayName("후보가 비면 빈 스냅샷을 저장하고 빈 리스트를 응답한다")
        void emptyCandidates() {
            // given
            Member requester = activeMember();
            given(memberQueryRepository.findScopesByMemberId(MEMBER_ID)).willReturn(List.of());
            given(snapshotRepository.findByMemberIdAndSlotAt(eq(MEMBER_ID), any()))
                    .willReturn(Optional.empty());
            given(recommendQueryRepository.findRecommendCandidates(any(), any(), eq(MEMBER_ID)))
                    .willReturn(List.of());

            // when
            List<OfficialPostCardResponse> result = recommendQueryService.getRecommendPosts(CONTEXT, requester);

            // then
            assertThat(result).isEmpty();
            then(scoringService).shouldHaveNoInteractions();
            then(interestRepository).shouldHaveNoInteractions();
            then(recommendQueryRepository).should(never())
                    .findStatsByPostIds(any(), anyInt(), anyInt(), any());
            then(recommendQueryRepository).should(never()).findCardsByIds(any());
            then(snapshotWriter).should().persist(eq(MEMBER_ID), any(), eq(List.of()));
        }

        @Test
        @DisplayName("writer 충돌 시 기존 스냅샷으로 fallback 한다")
        void writerConflictFallbackToExisting() {
            // given
            Member requester = activeMember();
            given(memberQueryRepository.findScopesByMemberId(MEMBER_ID)).willReturn(List.of());
            given(snapshotRepository.findByMemberIdAndSlotAt(eq(MEMBER_ID), any()))
                    .willReturn(Optional.empty())
                    .willReturn(Optional.of(FeedRecommendSnapshot.create(
                            MEMBER_ID, LocalDateTime.now(), List.of(9L, 8L))));
            given(recommendQueryRepository.findRecommendCandidates(any(), any(), eq(MEMBER_ID)))
                    .willReturn(List.of());
            willThrow(new DataIntegrityViolationException("uq_feed_recommend_snapshot"))
                    .given(snapshotWriter).persist(eq(MEMBER_ID), any(), eq(List.of()));

            OfficialPostCardRaw card8 = card(8L);
            OfficialPostCardRaw card9 = card(9L);
            given(recommendQueryRepository.findCardsByIds(eq(List.of(9L, 8L))))
                    .willReturn(List.of(card8, card9));
            given(cardResponseMapper.toResponse(card8)).willReturn(response(8L));
            given(cardResponseMapper.toResponse(card9)).willReturn(response(9L));

            // when
            List<OfficialPostCardResponse> result = recommendQueryService.getRecommendPosts(CONTEXT, requester);

            // then
            assertThat(result).extracting(OfficialPostCardResponse::postId).containsExactly(9L, 8L);
        }

        @Test
        @DisplayName("writer 충돌 + 기존 row 부재 시 원본 예외를 전파한다")
        void writerConflictAndNoExistingRow() {
            // given
            Member requester = activeMember();
            given(memberQueryRepository.findScopesByMemberId(MEMBER_ID)).willReturn(List.of());
            given(snapshotRepository.findByMemberIdAndSlotAt(eq(MEMBER_ID), any()))
                    .willReturn(Optional.empty());
            given(recommendQueryRepository.findRecommendCandidates(any(), any(), eq(MEMBER_ID)))
                    .willReturn(List.of());
            DataIntegrityViolationException original =
                    new DataIntegrityViolationException("uq_feed_recommend_snapshot");
            willThrow(original).given(snapshotWriter).persist(eq(MEMBER_ID), any(), eq(List.of()));

            // when / then
            assertThatThrownBy(() -> recommendQueryService.getRecommendPosts(CONTEXT, requester))
                    .isSameAs(original);
        }
    }

    private static Member activeMember() {
        return Member.join(
                "u@test.ac.kr", "user", "password", "nick",
                UNIVERSITY_ID, null, 2022, 3);
    }

    private static OfficialPostRecommendCandidateRaw candidate(long postId) {
        return new OfficialPostRecommendCandidateRaw(
                postId, "title-" + postId, 100L, "tag-" + postId,
                "summary-" + postId, "key-" + postId, LocalDate.of(2026, 5, 1));
    }

    private static OfficialPostCardRaw card(long postId) {
        return new OfficialPostCardRaw(
                postId, "title-" + postId, "tag-" + postId, "summary-" + postId,
                null, LocalDate.of(2026, 5, 1));
    }

    private static OfficialPostCardResponse response(long postId) {
        return new OfficialPostCardResponse(
                postId, "title-" + postId, "tag-" + postId, "summary-" + postId,
                null, LocalDate.of(2026, 5, 1));
    }
}
