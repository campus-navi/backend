package com.campusnavi.backend.notification.service;

import com.campusnavi.backend.global.common.AuthContext;
import com.campusnavi.backend.global.exception.BusinessException;
import com.campusnavi.backend.global.exception.ErrorCode;
import com.campusnavi.backend.notification.dto.MissedNoticeCard;
import com.campusnavi.backend.notification.entity.ActivityNotificationSnapshot;
import com.campusnavi.backend.notification.repository.ActivityNotificationSnapshotRepository;
import com.campusnavi.backend.notification.repository.NotificationQueryRepository;
import com.campusnavi.backend.official.post.dto.OfficialPostCardRaw;
import com.campusnavi.backend.official.post.dto.OfficialPostCardResponse;
import com.campusnavi.backend.official.post.repository.OfficialPostViewRepository;
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
    @Mock
    private OfficialPostCardResponseMapper cardResponseMapper;

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
        @DisplayName("미열람이 있는 missedDate만 카드로 반환한다")
        void onlyMissedDatesWithUnread() {
            LocalDate today = LocalDate.now();
            ActivityNotificationSnapshot newer = snapshot(today.minusDays(1), List.of(1L, 2L, 3L));
            ActivityNotificationSnapshot older = snapshot(today.minusDays(3), List.of(4L, 5L));

            given(snapshotRepository.findAllByMemberIdOrderByMissedDateDesc(MEMBER_ID))
                    .willReturn(List.of(newer, older));
            given(notificationQueryRepository.findValidPostIdsByIds(any()))
                    .willReturn(List.of(1L, 2L, 3L, 4L, 5L));
            given(officialPostViewRepository.findPostIdsByMemberIdAndPostIdIn(eq(MEMBER_ID), any()))
                    .willReturn(Set.of(2L, 4L, 5L));

            List<MissedNoticeCard> result = service.getActivityCards(CONTEXT);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).missedDate()).isEqualTo(today.minusDays(1));
            assertThat(result.get(0).count()).isEqualTo(2);
        }

        @Test
        @DisplayName("마감이 지나 유효하지 않은 공지는 count에서 제외한다")
        void excludesExpiredFromCount() {
            LocalDate missedDate = LocalDate.now().minusDays(1);
            ActivityNotificationSnapshot snap = snapshot(missedDate, List.of(1L, 2L, 3L));

            given(snapshotRepository.findAllByMemberIdOrderByMissedDateDesc(MEMBER_ID))
                    .willReturn(List.of(snap));
            // 2L은 마감이 지나 findValidPostIdsByIds 결과에서 빠짐
            given(notificationQueryRepository.findValidPostIdsByIds(any()))
                    .willReturn(List.of(1L, 3L));
            given(officialPostViewRepository.findPostIdsByMemberIdAndPostIdIn(eq(MEMBER_ID), any()))
                    .willReturn(Set.of());

            List<MissedNoticeCard> result = service.getActivityCards(CONTEXT);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).count()).isEqualTo(2);
        }

        @Test
        @DisplayName("모두 열람되어 count가 0인 missedDate는 카드에서 제외한다")
        void excludesZeroCountCard() {
            LocalDate missedDate = LocalDate.now().minusDays(1);
            ActivityNotificationSnapshot snap = snapshot(missedDate, List.of(10L, 11L));
            given(snapshotRepository.findAllByMemberIdOrderByMissedDateDesc(MEMBER_ID))
                    .willReturn(List.of(snap));
            given(notificationQueryRepository.findValidPostIdsByIds(any()))
                    .willReturn(List.of(10L, 11L));
            given(officialPostViewRepository.findPostIdsByMemberIdAndPostIdIn(eq(MEMBER_ID), any()))
                    .willReturn(Set.of(10L, 11L));

            List<MissedNoticeCard> result = service.getActivityCards(CONTEXT);

            assertThat(result).isEmpty();
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
        @DisplayName("snapshot.postIds 원본 순서를 유지하면서 미열람만 카드로 반환한다")
        void preservesOrderAndFilters() {
            LocalDate missedDate = LocalDate.now().minusDays(2);
            ActivityNotificationSnapshot snap = snapshot(missedDate, List.of(30L, 10L, 20L, 40L));

            given(snapshotRepository.findByMemberIdAndMissedDate(MEMBER_ID, missedDate))
                    .willReturn(java.util.Optional.of(snap));
            given(officialPostViewRepository.findPostIdsByMemberIdAndPostIdIn(eq(MEMBER_ID), any()))
                    .willReturn(Set.of(20L));
            given(notificationQueryRepository.findMissedCardsByIds(any()))
                    .willReturn(List.of(
                            cardRaw(10L, "ten", "공지"),
                            cardRaw(30L, "thirty", "행사"),
                            cardRaw(40L, "forty", "장학")));
            given(cardResponseMapper.toResponse(any()))
                    .willAnswer(inv -> response(inv.getArgument(0)));

            List<OfficialPostCardResponse> result = service.getActivityDetail(CONTEXT, missedDate);

            assertThat(result).extracting(OfficialPostCardResponse::postId).containsExactly(30L, 10L, 40L);
            assertThat(result).extracting(OfficialPostCardResponse::title).containsExactly("thirty", "ten", "forty");
            assertThat(result).extracting(OfficialPostCardResponse::tagName).containsExactly("행사", "공지", "장학");
        }

        @Test
        @DisplayName("모두 열람되면 카드 조회 없이 빈 리스트를 반환한다")
        void emptyWhenAllConsumed() {
            LocalDate missedDate = LocalDate.now().minusDays(1);
            ActivityNotificationSnapshot snap = snapshot(missedDate, List.of(1L, 2L));
            given(snapshotRepository.findByMemberIdAndMissedDate(MEMBER_ID, missedDate))
                    .willReturn(java.util.Optional.of(snap));
            given(officialPostViewRepository.findPostIdsByMemberIdAndPostIdIn(eq(MEMBER_ID), any()))
                    .willReturn(Set.of(1L, 2L));

            List<OfficialPostCardResponse> result = service.getActivityDetail(CONTEXT, missedDate);

            assertThat(result).isEmpty();
        }
    }

    private static ActivityNotificationSnapshot snapshot(LocalDate missedDate, List<Long> postIds) {
        return ActivityNotificationSnapshot.of(MEMBER_ID, missedDate, postIds);
    }

    private static OfficialPostCardRaw cardRaw(Long postId, String title, String tagName) {
        return new OfficialPostCardRaw(postId, title, tagName, "summary", "s3key", LocalDate.now());
    }

    private static OfficialPostCardResponse response(OfficialPostCardRaw r) {
        return new OfficialPostCardResponse(r.postId(), r.title(), r.tagName(),
                r.summary(), r.s3Key(), r.publishedAt());
    }
}
