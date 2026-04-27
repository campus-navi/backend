package com.campusnavi.backend.official.ai;

import com.campusnavi.backend.infra.ai.AiClient;
import com.campusnavi.backend.infra.storage.S3StorageService;
import com.campusnavi.backend.official.ai.dto.OfficialAiRequest;
import com.campusnavi.backend.official.ai.dto.OfficialAiResponse;
import com.campusnavi.backend.official.entity.OfficialAttachment;
import com.campusnavi.backend.official.entity.OfficialPost;
import com.campusnavi.backend.official.repository.OfficialAttachmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AiMetaProcessor {

    private final OfficialAttachmentRepository attachmentRepository;
    private final S3StorageService s3StorageService;
    private final AiClient aiClient;
    private final AiMetaService aiMetaService;

    public void process(OfficialPost post) {
        Long postId = post.getId();
        try {
            List<OfficialAttachment> attachments = attachmentRepository.findByPostId(postId);
            List<String> imageUrls = attachments.stream()
                    .filter(OfficialAttachment::isImage)
                    .map(OfficialAttachment::getS3Key)
                    .map(s3StorageService::resolveUrl)
                    .toList();

            List<String> attachmentUrls = attachments.stream()
                    .filter(a -> !a.isImage())
                    .map(OfficialAttachment::getS3Key)
                    .map(s3StorageService::resolveUrl)
                    .toList();

            OfficialAiRequest request = new OfficialAiRequest(postId, post.getStructuredText(), imageUrls, attachmentUrls);

            OfficialAiResponse response = aiClient.post("/ai/official/process", request, OfficialAiResponse.class);

            aiMetaService.saveResult(postId, response);
        } catch (Exception e) {
            aiMetaService.markFailed(postId, e.getMessage());
        }
    }
}
