package com.campusnavi.backend.notification.service;

import com.campusnavi.backend.global.common.AuthContext;
import com.campusnavi.backend.global.exception.BusinessException;
import com.campusnavi.backend.global.exception.ErrorCode;
import com.campusnavi.backend.notification.dto.MissedNotice;
import com.campusnavi.backend.notification.dto.MissedNoticeCard;
import com.campusnavi.backend.notification.dto.MissedNoticeRaw;
import com.campusnavi.backend.notification.entity.ActivityNotificationSnapshot;
import com.campusnavi.backend.notification.repository.ActivityNotificationSnapshotRepository;
import com.campusnavi.backend.notification.repository.NotificationQueryRepository;
import com.campusnavi.backend.official.post.repository.OfficialPostViewRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ActivityNotificationServiceTest {

    @Mock
    private ActivityNotificationSnapshotRepository snapshotRepository;
    @Mock
    private NotificationQueryRepository notificationQueryRepository;
    @Mock
    private OfficialPostViewRepository officialPostViewRepository;

    @InjectMocks
    private ActivityNotificationService service;

    private static final Long MEMBER_ID = 7L;
    private static final Long UNIVERSITY_ID = 100L;
    private static final AuthContext CONTEXT = new AuthContext(MEMBER_ID, UNIVERSITY_ID);

    @Nested
    @DisplayName("카드 목록 조회")
    class GetCards {

        @Test
        @DisplayName("스냅샷이 없으면 빈 리스트를 반환한다")
        void empty() {
            given(snapshotRepository.findAllByMemberIdOrderByMissedDateDesc(MEMBER_ID))
                    .willReturn(List.of());

            List<MissedNoticeCard> result = service.getActivityCards(CONTEXT);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("모든 missedDate를 카드로 반환하며 미열람 개수를 count로 표기한다")
        void allMissedDatesAsCards() {
            LocalDate today = LocalDate.now();
            ActivityNotificationSnapshot newer = snapshot(today.minusDays(1), List.of(1L, 2L, 3L));
            ActivityNotificationSnapshot older = snapshot(today.minusDays(3), List.of(4L, 5L));

            given(snapshotRepository.findAllByMemberIdOrderByMissedDateDesc(MEMBER_ID))
                    .willReturn(List.of(newer, older));
            given(notificationQueryRepository.findMissedNoticesByIds(any()))
                    .willReturn(List.of(raw(1L, "t1", "공지"), raw(2L, "t2", "공지"),
                            raw(3L, "t3", "공지"), raw(4L, "t4", "공지"), raw(5L, "t5", "공지")));
            given(officialPostViewRepository.findPostIdsByMemberIdAndPostIdIn(eq(MEMBER_ID), any()))
                    .willReturn(Set.of(2L, 4L, 5L));

            List<MissedNoticeCard> result = service.getActivityCards(CONTEXT);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).missedDate()).isEqualTo(today.minusDays(1));
            assertThat(result.get(0).count()).isEqualTo(2);
            assertThat(result.get(1).missedDate()).isEqualTo(today.minusDays(3));
            assertThat(result.get(1).count()).isEqualTo(0);
        }

        @Test
        @DisplayName("마감이 지나 유효하지 않은 공지는 count에서 제외한다")
        void excludesExpiredFromCount() {
            LocalDate missedDate = LocalDate.now().minusDays(1);
            ActivityNotificationSnapshot snap = snapshot(missedDate, List.of(1L, 2L, 3L));

            given(snapshotRepository.findAllByMemberIdOrderByMissedDateDesc(MEMBER_ID))
                    .willReturn(List.of(snap));
            // 2L은 마감이 지나 findMissedNoticesByIds 결과에서 빠짐
            given(notificationQueryRepository.findMissedNoticesByIds(any()))
                    .willReturn(List.of(raw(1L, "t1", "공지"), raw(3L, "t3", "공지")));
            given(officialPostViewRepository.findPostIdsByMemberIdAndPostIdIn(eq(MEMBER_ID), any()))
                    .willReturn(Set.of());

            List<MissedNoticeCard> result = service.getActivityCards(CONTEXT);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).count()).isEqualTo(2);
        }

        @Test
        @DisplayName("모두 열람된 missedDate도 count=0 카드로 반환한다")
        void allViewedReturnsZeroCountCard() {
            LocalDate missedDate = LocalDate.now().minusDays(1);
            ActivityNotificationSnapshot snap = snapshot(missedDate, List.of(10L, 11L));
            given(snapshotRepository.findAllByMemberIdOrderByMissedDateDesc(MEMBER_ID))
                    .willReturn(List.of(snap));
            given(notificationQueryRepository.findMissedNoticesByIds(any()))
                    .willReturn(List.of(raw(10L, "t10", "공지"), raw(11L, "t11", "공지")));
            given(officialPostViewRepository.findPostIdsByMemberIdAndPostIdIn(eq(MEMBER_ID), any()))
                    .willReturn(Set.of(10L, 11L));

            List<MissedNoticeCard> result = service.getActivityCards(CONTEXT);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).missedDate()).isEqualTo(missedDate);
            assertThat(result.get(0).count()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("상세 조회")
    class GetDetail {

        @Test
        @DisplayName("스냅샷이 없으면 ACTIVITY_NOTIFICATION_NOT_FOUND를 던진다")
        void notFound() {
            LocalDate missedDate = LocalDate.now().minusDays(1);
            given(snapshotRepository.findByMemberIdAndMissedDate(MEMBER_ID, missedDate))
                    .willReturn(java.util.Optional.empty());

            assertThatThrownBy(() -> service.getActivityDetail(CONTEXT, missedDate))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.ACTIVITY_NOTIFICATION_NOT_FOUND);
        }

        @Test
        @DisplayName("snapshot.postIds 원본 순서를 유지하면서 미열람만 반환한다")
        void preservesOrderAndFilters() {
            LocalDate missedDate = LocalDate.now().minusDays(2);
            ActivityNotificationSnapshot snap = snapshot(missedDate, List.of(30L, 10L, 20L, 40L));

            given(snapshotRepository.findByMemberIdAndMissedDate(MEMBER_ID, missedDate))
                    .willReturn(java.util.Optional.of(snap));
            given(officialPostViewRepository.findPostIdsByMemberIdAndPostIdIn(eq(MEMBER_ID), any()))
                    .willReturn(Set.of(20L));
            given(notificationQueryRepository.findMissedNoticesByIds(any()))
                    .willReturn(List.of(
                            raw(10L, "ten", "공지"),
                            raw(30L, "thirty", "행사"),
                            raw(40L, "forty", "장학")));

            List<MissedNotice> result = service.getActivityDetail(CONTEXT, missedDate);

            assertThat(result).extracting(MissedNotice::postId).containsExactly(30L, 10L, 40L);
            assertThat(result).extracting(MissedNotice::title).containsExactly("thirty", "ten", "forty");
            assertThat(result).extracting(MissedNotice::tagName).containsExactly("행사", "공지", "장학");
        }

        @Test
        @DisplayName("모두 열람되면 missed notice 조회 없이 빈 리스트를 반환한다")
        void emptyWhenAllConsumed() {
            LocalDate missedDate = LocalDate.now().minusDays(1);
            ActivityNotificationSnapshot snap = snapshot(missedDate, List.of(1L, 2L));
            given(snapshotRepository.findByMemberIdAndMissedDate(MEMBER_ID, missedDate))
                    .willReturn(java.util.Optional.of(snap));
            given(officialPostViewRepository.findPostIdsByMemberIdAndPostIdIn(eq(MEMBER_ID), any()))
                    .willReturn(Set.of(1L, 2L));

            List<MissedNotice> result = service.getActivityDetail(CONTEXT, missedDate);

            assertThat(result).isEmpty();
        }
    }

    private static ActivityNotificationSnapshot snapshot(LocalDate missedDate, List<Long> postIds) {
        return ActivityNotificationSnapshot.of(MEMBER_ID, missedDate, postIds);
    }

    private static MissedNoticeRaw raw(Long postId, String title, String tagName) {
        return new MissedNoticeRaw(postId, title, tagName, null, null);
    }
}
