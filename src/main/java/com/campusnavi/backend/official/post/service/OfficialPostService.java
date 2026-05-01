package com.campusnavi.backend.official.post.service;

import com.campusnavi.backend.global.common.ProcessingStatus;
import com.campusnavi.backend.global.exception.BusinessException;
import com.campusnavi.backend.global.exception.ErrorCode;
import com.campusnavi.backend.infra.storage.S3StorageService;
import com.campusnavi.backend.official.post.dto.AttachmentResponse;
import com.campusnavi.backend.official.post.dto.OfficialPostDetailResponse;
import com.campusnavi.backend.official.post.entity.OfficialAttachment;
import com.campusnavi.backend.official.post.entity.OfficialPost;
import com.campusnavi.backend.official.post.entity.OfficialPostAiMeta;
import com.campusnavi.backend.official.post.repository.OfficialAttachmentRepository;
import com.campusnavi.backend.official.post.repository.OfficialPostAiMetaRepository;
import com.campusnavi.backend.official.post.repository.OfficialPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OfficialPostService {

    private final OfficialPostRepository postRepository;
    private final OfficialPostAiMetaRepository aiMetaRepository;
    private final OfficialAttachmentRepository attachmentRepository;
    private final S3StorageService storageService;

    @Transactional(readOnly = true)
    public OfficialPostDetailResponse getDetail(Long postId) {
        OfficialPost post = postRepository.findByIdAndIsActiveTrue(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.OFFICIAL_POST_NOT_FOUND));

        OfficialPostAiMeta meta = aiMetaRepository.findByOfficialPostIdWithTag(postId, ProcessingStatus.DONE)
                .orElseThrow(() -> new BusinessException(ErrorCode.OFFICIAL_POST_NOT_READY));

        List<OfficialAttachment> all = attachmentRepository.findByPostIdOrderBySortOrderAsc(postId);

        String thumbnailUrl =all.stream()
                .filter(OfficialAttachment::isImage)
                .findFirst()
                .map(a -> storageService.resolveUrl(a.getS3Key()))
                .orElse(null);

        List<AttachmentResponse> files = all.stream()
                .filter(a -> !a.isImage())
                .map(a -> new AttachmentResponse(
                        a.getOriginalName(), storageService.resolveUrl(a.getS3Key())))
                .toList();

        return new OfficialPostDetailResponse(
                post.getId(),
                post.getTitle(),
                post.getPublisher(),
                post.getSourceUrl(),
                post.getPublishedAt(),
                meta.getTag() != null ? meta.getTag().getName() : null,
                meta.getSummary(),
                post.getHtmlContent(),
                meta.isApplicable(),
                meta.getStartDate(),
                meta.getStartTime(),
                meta.getEndDate(),
                meta.getEndTime(),
                meta.getEligibility(),
                meta.getApplyMethodType(),
                meta.getApplyMethodDetail(),
                meta.getRequiredDocuments(),
                meta.getContactPhone(),
                meta.getContactEmail(),
                thumbnailUrl,
                files
        );
    }
}
