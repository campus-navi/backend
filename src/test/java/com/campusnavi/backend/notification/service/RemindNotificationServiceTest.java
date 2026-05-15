package com.campusnavi.backend.notification.service;

import com.campusnavi.backend.global.common.AuthContext;
import com.campusnavi.backend.notification.dto.RemindNotice;
import com.campusnavi.backend.notification.repository.NotificationQueryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class RemindNotificationServiceTest {

    @Mock
    private NotificationQueryRepository notificationQueryRepository;

    @InjectMocks
    private RemindNotificationService service;

    private static final Long MEMBER_ID = 7L;
    private static final AuthContext CONTEXT = new AuthContext(MEMBER_ID, 100L);

    @Test
    @DisplayName("회원 id로 조회한 리마인드 목록을 그대로 반환한다")
    void returnsQueryResult() {
        List<RemindNotice> expected = List.of(
                new RemindNotice(1L, "공지 A", LocalDate.now().plusDays(1)),
                new RemindNotice(2L, "공지 B", LocalDate.now().plusDays(3)));
        given(notificationQueryRepository.findRemindNoticesByMemberId(MEMBER_ID)).willReturn(expected);

        List<RemindNotice> result = service.getRemindNotices(CONTEXT);

        assertThat(result).isEqualTo(expected);
        then(notificationQueryRepository).should().findRemindNoticesByMemberId(MEMBER_ID);
    }

    @Test
    @DisplayName("리마인드 대상이 없으면 빈 리스트를 반환한다")
    void returnsEmpty() {
        given(notificationQueryRepository.findRemindNoticesByMemberId(MEMBER_ID)).willReturn(List.of());

        assertThat(service.getRemindNotices(CONTEXT)).isEmpty();
    }
}
