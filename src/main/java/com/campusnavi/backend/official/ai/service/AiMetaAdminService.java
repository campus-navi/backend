package com.campusnavi.backend.official.ai.service;

import com.campusnavi.backend.global.common.ProcessingStatus;
import com.campusnavi.backend.global.exception.BusinessException;
import com.campusnavi.backend.global.exception.ErrorCode;
import com.campusnavi.backend.infra.ai.AiClient;
import com.campusnavi.backend.official.ai.dto.*;
import com.campusnavi.backend.official.post.entity.OfficialPost;
import com.campusnavi.backend.official.post.entity.OfficialPostAiMeta;
import com.campusnavi.backend.official.post.repository.OfficialPostAiMetaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiMetaAdminService {

    private static final int CHUNK_SIZE = 10;

    private final OfficialPostAiMetaRepository metaRepository;
    private final AiClient aiClient;
    private final AiMetaProcessor processor;
    private final AiMetaService aiMetaService;
    private final Executor adminTaskExecutor;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile LocalDateTime startedAt;
    private volatile LocalDateTime finishedAt;

    public void startProcessPendingAsync(int limit, Long campusId, Long departmentId) {
        if (!running.compareAndSet(false, true)) {
            throw new BusinessException(ErrorCode.AI_BATCH_ALREADY_RUNNING);
        }
        startedAt = LocalDateTime.now();
        finishedAt = null;
        try {
            adminTaskExecutor.execute(() -> {
                try {
                    processPendingBatch(limit, campusId, departmentId);
                } catch (Exception e) {
                    log.error("AI 배치 비동기 실행 실패: {}", e.getMessage(), e);
                } finally {
                    finishedAt = LocalDateTime.now();
                    running.set(false);
                }
            });
        } catch (RuntimeException e) {
            running.set(false);
            throw e;
        }
    }

    public AiMetaStatusResponse status() {
        return new AiMetaStatusResponse(
                running.get(),
                startedAt,
                finishedAt,
                metaRepository.countByStatus(ProcessingStatus.PENDING),
                metaRepository.countByStatus(ProcessingStatus.DONE),
                metaRepository.countByStatus(ProcessingStatus.FAILED)
        );
    }

    public BatchResult processPendingBatch(int limit, Long campusId, Long departmentId) {
        List<OfficialPostAiMeta> target = metaRepository.findPendingBatch(
                ProcessingStatus.PENDING, campusId, departmentId, PageRequest.of(0, limit));

        if (target.isEmpty()) return new BatchResult(0, 0);

        long success = 0;
        long failure = 0;
        for (int i = 0; i < target.size(); i += CHUNK_SIZE) {
            List<OfficialPost> posts = target.subList(i, Math.min(i + CHUNK_SIZE, target.size())).stream()
                    .map(OfficialPostAiMeta::getOfficialPost)
                    .toList();

            try {
                List<OfficialAiRequest> requests = processor.buildRequests(posts);

                OfficialAiBatchResponse batchResponse = aiClient.post(
                        "/ai/official/process/batch",
                        new OfficialAiBatchRequest(requests),
                        OfficialAiBatchResponse.class
                );

                for (OfficialAiBatchItemResponse item : batchResponse.results()) {
                    if (item.success()) {
                        aiMetaService.saveResult(item.postId(), item.result());
                        success++;
                    } else {
                        aiMetaService.markFailed(item.postId(), item.reason());
                        failure++;
                    }
                }
            } catch (Exception e) {
                String reason = buildFailureReason(e);
                log.error("AI 배치 청크 호출 실패 (대상 {}건): {}", posts.size(), reason, e);
                posts.forEach(post -> aiMetaService.markFailed(post.getId(), reason));
                failure += posts.size();
            }
        }

        log.info("AI 배치 완료: 대상 {}건, 성공 {}, 실패 {}", target.size(), success, failure);
        return new BatchResult(success, failure);
    }

    private String buildFailureReason(Exception e) {
        String detail = e instanceof RestClientResponseException rcre
                ? rcre.getStatusCode() + " " + rcre.getResponseBodyAsString()
                : e.getClass().getSimpleName() + ": " + e.getMessage();
        String reason = "AI 배치 호출 실패: " + detail;
        return reason.length() > 500 ? reason.substring(0, 500) : reason;
    }
}
