package com.campusnavi.backend.official.ai.service;

import com.campusnavi.backend.global.common.ProcessingStatus;
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

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiMetaAdminService {

    private final OfficialPostAiMetaRepository metaRepository;
    private final AiClient aiClient;
    private final AiMetaProcessor processor;
    private final AiMetaService aiMetaService;

    public BatchResult processPendingBatch(int limit) {
        List<OfficialPostAiMeta> target = metaRepository.findAllByStatus(ProcessingStatus.PENDING, PageRequest.of(0, limit))
                .getContent();

        if (target.isEmpty()) return new BatchResult(0, 0);

        List<OfficialPost> posts = target.stream()
                .map(OfficialPostAiMeta::getOfficialPost)
                .toList();

        try {
            List<OfficialAiRequest> requests = processor.buildRequests(posts);

            OfficialAiBatchResponse batchResponse = aiClient.post(
                    "/ai/official/process/batch",
                    new OfficialAiBatchRequest(requests),
                    OfficialAiBatchResponse.class
            );

            List<OfficialAiBatchItemResponse> results = batchResponse.results();
            for (OfficialAiBatchItemResponse item : results) {
                if (item.success()) {
                    aiMetaService.saveResult(item.postId(), item.result());
                } else {
                    aiMetaService.markFailed(item.postId(), item.reason());
                }
            }

            long successCount = results.stream().filter(OfficialAiBatchItemResponse::success).count();
            long failedCount = results.size() - successCount;

            return new BatchResult(successCount, failedCount);
        } catch (Exception e) {
            String reason = buildFailureReason(e);
            log.error("AI 배치 호출 실패 (대상 {}건): {}", posts.size(), reason, e);
            posts.forEach(post -> aiMetaService.markFailed(post.getId(), reason));
            return new BatchResult(0, posts.size());
        }
    }

    private String buildFailureReason(Exception e) {
        String detail = e instanceof RestClientResponseException rcre
                ? rcre.getStatusCode() + " " + rcre.getResponseBodyAsString()
                : e.getClass().getSimpleName() + ": " + e.getMessage();
        String reason = "AI 배치 호출 실패: " + detail;
        return reason.length() > 500 ? reason.substring(0, 500) : reason;
    }
}
