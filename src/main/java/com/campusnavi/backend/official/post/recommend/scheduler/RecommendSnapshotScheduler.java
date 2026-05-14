package com.campusnavi.backend.official.post.recommend.scheduler;

import com.campusnavi.backend.member.entity.Member;
import com.campusnavi.backend.member.entity.MemberRole;
import com.campusnavi.backend.member.repository.MemberRepository;
import com.campusnavi.backend.official.post.recommend.service.RecommendSnapshotBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;


@Slf4j
@Component
@RequiredArgsConstructor
public class RecommendSnapshotScheduler {

    private final MemberRepository memberRepository;
    private final RecommendSnapshotBuilder snapshotBuilder;
    private final RecommendSnapshotCleaner cleaner;

    private static final int CHUNK_SIZE = 500;
    private static final int RETENTION_DAYS = 3;

    @Scheduled(cron = "0 0 9,18 * * *", zone = "Asia/Seoul")
    public void rebuildAll() {
        long lastId = 0L;

        while (true) {
            List<Member> chunk = memberRepository.findActiveAfterIdExcludingRole(
                    lastId, MemberRole.ADMIN, PageRequest.of(0, CHUNK_SIZE));
            if (chunk.isEmpty()) break;

            for (Member member : chunk) {
                try {
                    snapshotBuilder.computeAndUpsert(member);
                } catch (RuntimeException e) {
                    log.warn("추천 스냅샷 갱신 실패. memberId={}", member.getId(), e);
                }
            }
            lastId = chunk.getLast().getId();
        }

        cleaner.cleanupOlderThan(LocalDateTime.now().minusDays(RETENTION_DAYS));
    }
}
