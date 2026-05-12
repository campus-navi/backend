package com.campusnavi.backend.official.post.service;

import com.campusnavi.backend.infra.storage.S3StorageService;
import com.campusnavi.backend.official.post.dto.OfficialPostCardRaw;
import com.campusnavi.backend.official.post.dto.OfficialPostCardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OfficialPostCardResponseMapper {

    private final S3StorageService s3StorageService;

    public OfficialPostCardResponse toResponse(OfficialPostCardRaw raw) {
        return new OfficialPostCardResponse(
                raw.postId(),
                raw.title(),
                raw.tagName(),
                raw.summary(),
                resolveImageUrl(raw.s3Key()),
                raw.publishedAt()
        );
    }

    private String resolveImageUrl(String s3Key) {
        if (s3Key == null || s3Key.isBlank()) return null;
        return s3StorageService.resolveUrl(s3Key);
    }
}
