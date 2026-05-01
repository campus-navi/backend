package com.campusnavi.backend.official.post.service;

import com.campusnavi.backend.global.common.AuthContext;
import com.campusnavi.backend.global.exception.BusinessException;
import com.campusnavi.backend.global.exception.ErrorCode;
import com.campusnavi.backend.infra.storage.S3StorageService;
import com.campusnavi.backend.official.post.dto.AttachmentDownloadResponse;
import com.campusnavi.backend.official.post.entity.OfficialAttachment;
import com.campusnavi.backend.official.post.entity.OfficialAttachmentDownload;
import com.campusnavi.backend.official.post.repository.OfficialAttachmentDownloadRepository;
import com.campusnavi.backend.official.post.repository.OfficialAttachmentRepository;
import com.campusnavi.backend.official.post.repository.OfficialPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Transactional
public class OfficialAttachmentDownloadService {

    private static final Duration DOWNLOAD_URL_TTL = Duration.ofMinutes(10);

    private final OfficialPostRepository postRepository;
    private final OfficialAttachmentRepository attachmentRepository;
    private final OfficialAttachmentDownloadRepository downloadRepository;
    private final S3StorageService storageService;

    public AttachmentDownloadResponse issueDownloadUrl(Long postId, Long attachmentId, AuthContext context) {
        if (!postRepository.existsActiveByIdAndUniversityScope(postId, context.universityId())) {
            throw new BusinessException(ErrorCode.OFFICIAL_POST_NOT_FOUND);
        }

        OfficialAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.OFFICIAL_ATTACHMENT_NOT_FOUND));

        if (!attachment.getPost().getId().equals(postId)) {
            throw new BusinessException(ErrorCode.OFFICIAL_ATTACHMENT_NOT_FOUND);
        }

        String downloadUrl = storageService.generateGetPresignedUrl(
                attachment.getS3Key(), attachment.getOriginalName(), DOWNLOAD_URL_TTL);

        downloadRepository.save(OfficialAttachmentDownload.create(
                context.memberId(), attachmentId, postId));

        return new AttachmentDownloadResponse(downloadUrl, DOWNLOAD_URL_TTL.toSeconds());
    }
}
