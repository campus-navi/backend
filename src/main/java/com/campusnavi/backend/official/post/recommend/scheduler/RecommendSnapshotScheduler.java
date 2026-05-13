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

import java.util.List;


@Slf4j
@Component
@RequiredArgsConstructor
public class RecommendSnapshotScheduler {

    private final MemberRepository memberRepository;
    private final RecommendSnapshotBuilder snapshotBuilder;

    private static final int CHUNK_SIZE = 500;

    @Scheduled(cron = "0 0 9,18 * * *", zone = "Asia/Seoul")
    public void rebuildAll() {
        long lastId = 0L;
        int totalProcessed = 0;

        while (true) {
            List<Member> chunk = memberRepository.findActiveAfterIdExcludingRole(
                    lastId, MemberRole.ADMIN, PageRequest.of(0, CHUNK_SIZE));
            if (chunk.isEmpty()) break;

            for (Member member : chunk) {
                try {
                    snapshotBuilder.computeAndUpsert(member);
                    totalProcessed++;
                } catch (RuntimeException e) {
                    log.warn("snapshot upsert failed for member {}", member.getId(), e);
                }
            }
            lastId = chunk.get(chunk.size() - 1).getId();
        }

        log.info("recommend snapshot rebuild done. processed={}", totalProcessed);
    }
}
