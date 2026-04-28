package com.campusnavi.backend.official.ai.service;

import com.campusnavi.backend.infra.ai.AiClient;
import com.campusnavi.backend.infra.storage.S3StorageService;
import com.campusnavi.backend.official.ai.dto.OfficialAiRequest;
import com.campusnavi.backend.official.ai.dto.OfficialAiResponse;
import com.campusnavi.backend.official.domain.entity.OfficialAttachment;
import com.campusnavi.backend.official.domain.entity.OfficialPost;
import com.campusnavi.backend.official.domain.repository.OfficialAttachmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
            List<OfficialAttachment> attachments = attachmentRepository.findByPostId(post.getId());
            OfficialAiRequest request = toRequest(post, attachments);
            OfficialAiResponse response = aiClient.post("/ai/official/process", request, OfficialAiResponse.class);
            aiMetaService.saveResult(postId, response);
        } catch (Exception e) {
            aiMetaService.markFailed(postId, e.getMessage());
        }
    }

    public List<OfficialAiRequest> buildRequests(List<OfficialPost> posts) {
        List<Long> postIds = posts.stream().map(OfficialPost::getId).toList();
        Map<Long, List<OfficialAttachment>> byPost = attachmentRepository.findByPostIdIn(postIds)
                .stream()
                .collect(Collectors.groupingBy(a -> a.getPost().getId()));
        return posts.stream()
                .map(post -> toRequest(post, byPost.getOrDefault(post.getId(), List.of())))
                .toList();
    }

    private OfficialAiRequest toRequest(OfficialPost post, List<OfficialAttachment> attachments) {
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
        return new OfficialAiRequest(post.getId(), post.getStructuredText(), imageUrls, attachmentUrls);
    }
}
