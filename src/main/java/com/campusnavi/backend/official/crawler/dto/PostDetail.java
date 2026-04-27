package com.campusnavi.backend.official.crawler.dto;

import java.util.List;

public record PostDetail(
        String title,
        String structuredText,
        String rawHtml,
        List<FileInfo> images,
        List<FileInfo> attachments
) {
}
