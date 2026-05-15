package com.campusnavi.backend.notification.service;

import com.campusnavi.backend.global.common.AuthContext;
import com.campusnavi.backend.notification.dto.RemindNotice;
import com.campusnavi.backend.notification.repository.NotificationQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RemindNotificationService {

    private final NotificationQueryRepository notificationQueryRepository;

    public List<RemindNotice> getRemindNotices(AuthContext context) {
        return notificationQueryRepository.findRemindNoticesByMemberId(context.memberId());
    }
}
