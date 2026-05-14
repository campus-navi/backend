package com.campusnavi.backend.official.post.recommend.service;

import com.campusnavi.backend.global.common.AuthContext;
import com.campusnavi.backend.member.entity.Member;
import com.campusnavi.backend.official.post.dto.OfficialPostCardRaw;
import com.campusnavi.backend.official.post.dto.OfficialPostCardResponse;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class RecommendQueryServiceTest {

    @Mock
    private FeedRecommendSnapshotRepository snapshotRepository;

    @Mock
    private RecommendQueryRepository recommendQueryRepository;

    @Mock
    private RecommendSnapshotBuilder snapshotBuilder;

    @Mock
    private OfficialPostCardResponseMapper cardResponseMapper;

    @InjectMocks
    private RecommendQueryService recommendQueryService;

    private static final Long MEMBER_ID = 1L;
    private static final Long UNIVERSITY_ID = 100L;
    private static final AuthContext CONTEXT = new AuthContext(MEMBER_ID, UNIVERSITY_ID);

    @Nested
    @DisplayName("getRecommendPosts")
    class GetRecommendPosts {

        @Test
        @DisplayName("스냅샷 hit 시 저장된 순서대로 카드를 응답하고 builder를 호출하지 않는다")
        void snapshotHit() {
            // given
            Member requester = activeMember();
            FeedRecommendSnapshot snapshot = mock(FeedRecommendSnapshot.class);
            given(snapshot.getPostIds()).willReturn(List.of(2L, 1L));
            given(snapshotRepository.findFirstByMemberIdOrderBySlotAtDesc(MEMBER_ID))
                    .willReturn(Optional.of(snapshot));

            OfficialPostCardRaw card1 = card(1L);
            OfficialPostCardRaw card2 = card(2L);
            given(recommendQueryRepository.findCardsByIds(eq(List.of(2L, 1L))))
                    .willReturn(List.of(card1, card2));
            given(cardResponseMapper.toResponse(card1)).willReturn(response(1L));
            given(cardResponseMapper.toResponse(card2)).willReturn(response(2L));

            // when
            List<OfficialPostCardResponse> result =
                    recommendQueryService.getRecommendPosts(CONTEXT, requester);

            // then
            assertThat(result).extracting(OfficialPostCardResponse::postId).containsExactly(2L, 1L);
            then(snapshotBuilder).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("스냅샷 row가 빈 배열이면 miss로 취급해 builder를 호출한다")
        void emptySnapshotTreatedAsMiss() {
            // given

            Member requester = activeMember();
            FeedRecommendSnapshot emptySnapshot = mock(FeedRecommendSnapshot.class);
            given(emptySnapshot.getPostIds()).willReturn(List.of());
            given(snapshotRepository.findFirstByMemberIdOrderBySlotAtDesc(MEMBER_ID))
                    .willReturn(Optional.of(emptySnapshot));
            given(snapshotBuilder.computeAndUpsert(requester)).willReturn(List.of(7L));

            OfficialPostCardRaw card = card(7L);
            given(recommendQueryRepository.findCardsByIds(eq(List.of(7L))))
                    .willReturn(List.of(card));
            given(cardResponseMapper.toResponse(card)).willReturn(response(7L));

            // when
            List<OfficialPostCardResponse> result =
                    recommendQueryService.getRecommendPosts(CONTEXT, requester);

            // then
            assertThat(result).extracting(OfficialPostCardResponse::postId).containsExactly(7L);
            then(snapshotBuilder).should().computeAndUpsert(requester);
        }

        @Test
        @DisplayName("스냅샷 miss 시 builder가 계산한 결과로 카드를 응답한다")
        void snapshotMiss() {
            // given
            Member requester = activeMember();
            given(snapshotRepository.findFirstByMemberIdOrderBySlotAtDesc(MEMBER_ID)).willReturn(Optional.empty());
            given(snapshotBuilder.computeAndUpsert(requester)).willReturn(List.of(7L));

            OfficialPostCardRaw card = card(7L);
            given(recommendQueryRepository.findCardsByIds(eq(List.of(7L))))
                    .willReturn(List.of(card));
            given(cardResponseMapper.toResponse(card)).willReturn(response(7L));

            // when
            List<OfficialPostCardResponse> result =
                    recommendQueryService.getRecommendPosts(CONTEXT, requester);

            // then
            assertThat(result).extracting(OfficialPostCardResponse::postId).containsExactly(7L);
            then(snapshotBuilder).should().computeAndUpsert(requester);
        }

        @Test
        @DisplayName("builder가 빈 결과를 반환하면 카드 조회 없이 빈 리스트를 응답한다")
        void emptyAfterBuilder() {
            // given
            Member requester = activeMember();
            given(snapshotRepository.findFirstByMemberIdOrderBySlotAtDesc(MEMBER_ID)).willReturn(Optional.empty());
            given(snapshotBuilder.computeAndUpsert(requester)).willReturn(List.of());

            // when
            List<OfficialPostCardResponse> result =
                    recommendQueryService.getRecommendPosts(CONTEXT, requester);

            // then
            assertThat(result).isEmpty();
            then(recommendQueryRepository).should(never()).findCardsByIds(any());
        }
    }

    private static Member activeMember() {
        return Member.join(
                "u@test.ac.kr", "user", "password", "nick",
                UNIVERSITY_ID, null, 2022, 3);
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
