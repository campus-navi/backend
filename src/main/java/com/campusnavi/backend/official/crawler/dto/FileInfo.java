package com.campusnavi.backend.official.crawler.dto;

public record FileInfo(
        String originalName,
        String originalUrl,
        String contentType) {
}
