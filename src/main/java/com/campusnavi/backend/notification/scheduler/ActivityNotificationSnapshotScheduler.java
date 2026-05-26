package com.campusnavi.backend.notification.scheduler;

import com.campusnavi.backend.member.entity.Member;
import com.campusnavi.backend.member.entity.MemberRole;
import com.campusnavi.backend.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityNotificationSnapshotScheduler {

    private final MemberRepository memberRepository;
    private final ActivityNotificationSnapshotWriter writer;

    private static final int CHUNK_SIZE = 500;
    private static final int RETENTION_DAYS = 30;

    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
    public void dispatch() {
        LocalDate missedDate = LocalDate.now().minusDays(1);

        long lastMemberId = 0L;
        while (true) {
            List<Member> chunk = memberRepository.findActiveAfterIdExcludingRole(
                    lastMemberId, MemberRole.ADMIN, PageRequest.of(0, CHUNK_SIZE));
            if (chunk.isEmpty()) break;
            try {
                writer.writeChunk(chunk, missedDate);
            } catch (RuntimeException e) {
                log.warn("활동 알림 스냅샷 청크 처리 실패. lastMemberId={}", lastMemberId, e);
            }
            lastMemberId = chunk.getLast().getId();
        }

        writer.cleanupOlderThan(LocalDate.now().minusDays(RETENTION_DAYS));
    }
}
