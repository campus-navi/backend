package com.campusnavi.backend.official.post.dto;

import java.util.List;

public record RemindBulkDeleteResponse(
        int deletedCount,
        List<Long> deletedPostIds
) {
}
