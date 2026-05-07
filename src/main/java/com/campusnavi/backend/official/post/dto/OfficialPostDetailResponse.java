package com.campusnavi.backend.official.post.dto;

import com.campusnavi.backend.official.post.entity.ApplyMethodType;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record OfficialPostDetailResponse(
        Long postId,
        String title,
        String publisher,
        String sourceUrl,
        LocalDate publishedAt,
        String tagName,
        String summary,
        String contentHtml,
        boolean isApplicable,
        LocalDate startDate,
        LocalTime startTime,
        LocalDate endDate,
        LocalTime endTime,
        String eligibility,
        ApplyMethodType applyMethodType,
        String applyMethodDetail,
        String requiredDocuments,
        String contactPhone,
        String contactEmail,
        List<String> imageUrls,
        List<AttachmentResponse> attachments,
        boolean hasUnreadAttachments,
        boolean isScrapped,
        boolean isNotificationOn
) {
}
