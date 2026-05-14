package com.campusnavi.backend.notification.scheduler;

import com.campusnavi.backend.member.entity.Member;
import com.campusnavi.backend.notification.entity.ActivityNotificationSnapshot;
import com.campusnavi.backend.notification.repository.ActivityNotificationSnapshotRepository;
import com.campusnavi.backend.official.post.entity.OfficialPostView;
import com.campusnavi.backend.official.post.recommend.repository.FeedRecommendSnapshotRepository;
import com.campusnavi.backend.official.post.repository.OfficialPostViewRepository;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ActivityNotificationSnapshotWriterTest {

    @Mock
    private FeedRecommendSnapshotRepository recommendSnapshotRepository;

    @Mock
    private OfficialPostViewRepository viewRepository;

    @Mock
    private ActivityNotificationSnapshotRepository snapshotRepository;

    @InjectMocks
    private ActivityNotificationSnapshotWriter writer;

    private static final LocalDate MISSED_DATE = LocalDate.of(2026, 5, 13);
    private static final LocalDateTime WINDOW_START = MISSED_DATE.atTime(9, 0);
    private static final LocalDateTime WINDOW_END = WINDOW_START.plusDays(1);

    @Nested
    @DisplayName("writeChunk")
    class WriteChunk {

        @Test
        @DisplayName("후보 중 미열람 포스트만 저장한다")
        void capturesMissedPosts() {
            // given
            Member m1 = member(1L);
            given(snapshotRepository.findMemberIdsByMissedDateAndMemberIdIn(
                    eq(MISSED_DATE), eq(List.of(1L))))
                    .willReturn(Set.of());
            given(recommendSnapshotRepository.findRawByMemberIdsAndSlotRange(
                    eq(List.of(1L)), eq(WINDOW_START), eq(WINDOW_END)))
                    .willReturn(List.of(
                            row(1L, 10L),
                            row(1L, 11L),
                            row(1L, 12L)));
            given(viewRepository.findByMemberIdInAndPostIdIn(eq(List.of(1L)), anyCollection()))
                    .willReturn(List.of(view(1L, 11L)));

            // when
            writer.writeChunk(List.of(m1), MISSED_DATE);

            // then
            ArgumentCaptor<List<ActivityNotificationSnapshot>> captor = captor();
            then(snapshotRepository).should().saveAll(captor.capture());
            List<ActivityNotificationSnapshot> saved = captor.getValue();
            assertThat(saved).hasSize(1);
            assertThat(saved.get(0).getMemberId()).isEqualTo(1L);
            assertThat(saved.get(0).getMissedDate()).isEqualTo(MISSED_DATE);
            assertThat(saved.get(0).getPostIds()).containsExactlyInAnyOrder(10L, 12L);
        }

        @Test
        @DisplayName("후보가 비면 view 조회/저장을 모두 생략한다")
        void skipWhenNoCandidates() {
            // given
            Member m1 = member(1L);
            given(snapshotRepository.findMemberIdsByMissedDateAndMemberIdIn(
                    eq(MISSED_DATE), eq(List.of(1L))))
                    .willReturn(Set.of());
            given(recommendSnapshotRepository.findRawByMemberIdsAndSlotRange(
                    eq(List.of(1L)), eq(WINDOW_START), eq(WINDOW_END)))
                    .willReturn(List.of());

            // when
            writer.writeChunk(List.of(m1), MISSED_DATE);

            // then
            then(viewRepository).shouldHaveNoInteractions();
            then(snapshotRepository).should(never()).saveAll(any());
        }

        @Test
        @DisplayName("모든 후보를 열람한 멤버는 저장 대상에서 제외된다")
        void skipsFullyViewedMember() {
            // given
            Member m1 = member(1L);
            Member m2 = member(2L);
            given(snapshotRepository.findMemberIdsByMissedDateAndMemberIdIn(
                    eq(MISSED_DATE), eq(List.of(1L, 2L))))
                    .willReturn(Set.of());
            given(recommendSnapshotRepository.findRawByMemberIdsAndSlotRange(
                    eq(List.of(1L, 2L)), eq(WINDOW_START), eq(WINDOW_END)))
                    .willReturn(List.of(
                            row(1L, 10L),
                            row(2L, 20L),
                            row(2L, 21L)));
            given(viewRepository.findByMemberIdInAndPostIdIn(eq(List.of(1L, 2L)), anyCollection()))
                    .willReturn(List.of(view(1L, 10L)));

            // when
            writer.writeChunk(List.of(m1, m2), MISSED_DATE);

            // then
            ArgumentCaptor<List<ActivityNotificationSnapshot>> captor = captor();
            then(snapshotRepository).should().saveAll(captor.capture());
            List<ActivityNotificationSnapshot> saved = captor.getValue();
            assertThat(saved).hasSize(1);
            assertThat(saved.get(0).getMemberId()).isEqualTo(2L);
            assertThat(saved.get(0).getPostIds()).containsExactlyInAnyOrder(20L, 21L);
        }

        @Test
        @DisplayName("이미 같은 missedDate에 row가 있는 멤버는 후속 처리에서 제외된다")
        void skipsAlreadyProcessedMember() {
            // given — m1은 이미 처리됨, m2만 신규 처리 대상
            Member m1 = member(1L);
            Member m2 = member(2L);
            given(snapshotRepository.findMemberIdsByMissedDateAndMemberIdIn(
                    eq(MISSED_DATE), eq(List.of(1L, 2L))))
                    .willReturn(Set.of(1L));
            given(recommendSnapshotRepository.findRawByMemberIdsAndSlotRange(
                    eq(List.of(2L)), eq(WINDOW_START), eq(WINDOW_END)))
                    .willReturn(List.<Object[]>of(row(2L, 20L)));
            given(viewRepository.findByMemberIdInAndPostIdIn(eq(List.of(2L)), anyCollection()))
                    .willReturn(List.of());

            // when
            writer.writeChunk(List.of(m1, m2), MISSED_DATE);

            // then
            ArgumentCaptor<List<ActivityNotificationSnapshot>> captor = captor();
            then(snapshotRepository).should().saveAll(captor.capture());
            List<ActivityNotificationSnapshot> saved = captor.getValue();
            assertThat(saved).hasSize(1);
            assertThat(saved.get(0).getMemberId()).isEqualTo(2L);
        }

        @Test
        @DisplayName("청크 전원이 이미 처리됐으면 recommend/view 조회를 생략한다")
        void skipWhenAllAlreadyProcessed() {
            // given
            Member m1 = member(1L);
            given(snapshotRepository.findMemberIdsByMissedDateAndMemberIdIn(
                    eq(MISSED_DATE), eq(List.of(1L))))
                    .willReturn(Set.of(1L));

            // when
            writer.writeChunk(List.of(m1), MISSED_DATE);

            // then
            then(recommendSnapshotRepository).shouldHaveNoInteractions();
            then(viewRepository).shouldHaveNoInteractions();
            then(snapshotRepository).should(never()).saveAll(any());
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static ArgumentCaptor<List<ActivityNotificationSnapshot>> captor() {
        return (ArgumentCaptor) ArgumentCaptor.forClass(List.class);
    }

    private static Object[] row(long memberId, long postId) {
        return new Object[]{memberId, postId};
    }

    private static OfficialPostView view(long memberId, long postId) {
        try {
            var ctor = OfficialPostView.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            OfficialPostView v = ctor.newInstance();
            Field memberIdField = OfficialPostView.class.getDeclaredField("memberId");
            memberIdField.setAccessible(true);
            memberIdField.set(v, memberId);
            Field postIdField = OfficialPostView.class.getDeclaredField("postId");
            postIdField.setAccessible(true);
            postIdField.set(v, postId);
            return v;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Member member(Long id) {
        Member m = Member.join("u" + id + "@test.ac.kr", "u" + id, "pw", "nick" + id,
                100L, null, 2022, 3);
        try {
            Field idField = Member.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(m, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return m;
    }
}
