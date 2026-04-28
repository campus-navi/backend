package com.campusnavi.backend.official.ai.service;

import com.campusnavi.backend.global.common.ProcessingStatus;
import com.campusnavi.backend.infra.ai.AiClient;
import com.campusnavi.backend.official.ai.dto.*;
import com.campusnavi.backend.official.domain.entity.OfficialPost;
import com.campusnavi.backend.official.domain.entity.OfficialPostAiMeta;
import com.campusnavi.backend.official.domain.repository.OfficialPostAiMetaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

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
    }
}
