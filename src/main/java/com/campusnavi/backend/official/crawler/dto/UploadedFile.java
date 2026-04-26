package com.campusnavi.backend.official.crawler.dto;

public record UploadedFile(
        String originalUrl,
        String originalName,
        String s3Key,
        String contentType
) {
}
