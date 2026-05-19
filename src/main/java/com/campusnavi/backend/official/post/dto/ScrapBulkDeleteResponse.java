package com.campusnavi.backend.official.post.dto;

import java.util.List;

public record ScrapBulkDeleteResponse(
        int deletedCount,
        List<Long> deletedPostIds
) {
}
