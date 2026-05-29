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
    private static final int MAX_CHUNK_RETRY = 2;

    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
    public void dispatch() {
        LocalDate missedDate = LocalDate.now().minusDays(1);

        long lastMemberId = 0L;
        int processed = 0;
        int failed = 0;
        while (true) {
            List<Member> chunk = memberRepository.findActiveAfterIdExcludingRole(
                    lastMemberId, MemberRole.ADMIN, PageRequest.of(0, CHUNK_SIZE));
            if (chunk.isEmpty()) break;

            if (writeChunkWithRetry(chunk, missedDate)) {
                processed++;
            } else {
                failed++;
                log.error("활동 알림 등록 영구 실패. lastMemberId={}, size={}", lastMemberId, chunk.size());
            }
            lastMemberId = chunk.getLast().getId();
        }

        writer.cleanupOlderThan(LocalDate.now().minusDays(RETENTION_DAYS));
        log.info("활동 알림 등록 완료: 성공 청크 {}, 실패 청크 {}", processed, failed);
    }

    private boolean writeChunkWithRetry(List<Member> chunk, LocalDate missedDate) {
        for (int attempt = 1; attempt <= MAX_CHUNK_RETRY; attempt++) {
            try {
                writer.writeChunk(chunk, missedDate);
                return true;
            } catch (RuntimeException e) {
                log.warn("활동 알림 등록 실패(시도 {}/{})", attempt, MAX_CHUNK_RETRY, e);
            }
        }
        return false;
    }
}
