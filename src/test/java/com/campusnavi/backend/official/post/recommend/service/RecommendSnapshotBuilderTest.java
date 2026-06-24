package com.campusnavi.backend.official.post.recommend.service;

import com.campusnavi.backend.member.dto.MemberScope;
import com.campusnavi.backend.member.entity.Member;
import com.campusnavi.backend.member.repository.MemberInterestRepository;
import com.campusnavi.backend.member.repository.MemberQueryRepository;
import com.campusnavi.backend.official.post.dto.OfficialPostRecommendCandidateRaw;
import com.campusnavi.backend.official.post.recommend.entity.FeedRecommendSnapshot;
import com.campusnavi.backend.official.post.recommend.entity.FeedRecommendSnapshotItem;
import com.campusnavi.backend.official.post.recommend.repository.FeedRecommendSnapshotItemRepository;
import com.campusnavi.backend.official.post.recommend.repository.FeedRecommendSnapshotRepository;
import com.campusnavi.backend.official.post.recommend.repository.RecommendQueryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class RecommendSnapshotBuilderTest {

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
    private FeedRecommendSnapshotItemRepository itemRepository;

    @InjectMocks
    private RecommendSnapshotBuilder builder;

    private static final Long MEMBER_ID = 1L;
    private static final Long UNIVERSITY_ID = 100L;
    private static final Long DEPT_ID = 10L;

    @Nested
    @DisplayName("computeAndUpsert")
    class ComputeAndUpsert {

        @Test
        @DisplayName("후보가 있으면 snapshot을 저장하고 items를 순서대로 저장한다")
        void upsertWithCandidates() {
            // given
            Member requester = activeMember();
            stubScope();
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
            FeedRecommendSnapshot snapshot = mock(FeedRecommendSnapshot.class);
            given(snapshotRepository.findByMemberIdAndSlotAt(eq(MEMBER_ID), any())).willReturn(Optional.of(snapshot));

            // when
            List<Long> ranked = builder.computeAndUpsert(requester);

            // then
            assertThat(ranked).containsExactly(7L);
            then(itemRepository).should().deleteBySnapshot(snapshot);
            @SuppressWarnings({"unchecked", "rawtypes"})
            ArgumentCaptor<List<FeedRecommendSnapshotItem>> captor =
                    (ArgumentCaptor) ArgumentCaptor.forClass(List.class);
            then(itemRepository).should().saveAll(captor.capture());
            List<FeedRecommendSnapshotItem> items = captor.getValue();
            assertThat(items).hasSize(1);
            assertThat(items.get(0).getPostId()).isEqualTo(7L);
            assertThat(items.get(0).getSortOrder()).isEqualTo(0);
        }

        @Test
        @DisplayName("ranked가 여러 건이면 순서대로 sortOrder가 부여된다")
        void upsertPreservesOrder() {
            // given
            Member requester = activeMember();
            stubScope();
            OfficialPostRecommendCandidateRaw c1 = candidate(3L);
            OfficialPostRecommendCandidateRaw c2 = candidate(1L);
            OfficialPostRecommendCandidateRaw c3 = candidate(2L);
            given(recommendQueryRepository.findRecommendCandidates(any(), any(), eq(MEMBER_ID)))
                    .willReturn(List.of(c1, c2, c3));
            given(recommendQueryRepository.findStatsByPostIds(any(), anyInt(), anyInt(), any()))
                    .willReturn(List.of());
            given(interestRepository.findTagIdsByMemberId(MEMBER_ID)).willReturn(List.of());
            given(memberQueryRepository.countActiveMembersInDepartments(any())).willReturn(0L);
            given(scoringService.rank(any(), any(), any(), eq(0L)))
                    .willReturn(List.of(c1, c2, c3));
            FeedRecommendSnapshot snapshot = mock(FeedRecommendSnapshot.class);
            given(snapshotRepository.findByMemberIdAndSlotAt(eq(MEMBER_ID), any())).willReturn(Optional.of(snapshot));

            // when
            builder.computeAndUpsert(requester);

            // then
            @SuppressWarnings({"unchecked", "rawtypes"})
            ArgumentCaptor<List<FeedRecommendSnapshotItem>> captor =
                    (ArgumentCaptor) ArgumentCaptor.forClass(List.class);
            then(itemRepository).should().saveAll(captor.capture());
            List<FeedRecommendSnapshotItem> items = captor.getValue();
            assertThat(items).extracting(FeedRecommendSnapshotItem::getPostId).containsExactly(3L, 1L, 2L);
            assertThat(items).extracting(i -> (int) i.getSortOrder()).containsExactly(0, 1, 2);
        }

        @Test
        @DisplayName("후보가 비면 snapshot/items 저장 없이 빈 결과를 반환한다")
        void skipUpsertWhenEmpty() {
            // given
            Member requester = activeMember();
            stubScope();
            given(recommendQueryRepository.findRecommendCandidates(any(), any(), eq(MEMBER_ID)))
                    .willReturn(List.of());

            // when
            List<Long> ranked = builder.computeAndUpsert(requester);

            // then
            assertThat(ranked).isEmpty();
            then(recommendQueryRepository).should(never())
                    .findStatsByPostIds(any(), anyInt(), anyInt(), any());
            then(interestRepository).shouldHaveNoInteractions();
            then(scoringService).shouldHaveNoInteractions();
            then(snapshotRepository).should(never()).insertIfAbsent(any(), any());
            then(itemRepository).shouldHaveNoInteractions();
        }
    }

    private void stubScope() {
        MemberScope scope = new MemberScope(null, null, DEPT_ID);
        given(memberQueryRepository.findScopesByMemberId(MEMBER_ID)).willReturn(List.of(scope));
    }

    private static Member activeMember() {
        Member m = Member.join(
                "u@test.ac.kr", "user", "password", "nick",
                UNIVERSITY_ID, null, 2022, 3);
        try {
            Field idField = Member.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(m, MEMBER_ID);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return m;
    }

    private static OfficialPostRecommendCandidateRaw candidate(long postId) {
        return new OfficialPostRecommendCandidateRaw(
                postId, "title-" + postId, 100L, "tag-" + postId,
                "summary-" + postId, "key-" + postId, LocalDate.of(2026, 5, 1));
    }
}
