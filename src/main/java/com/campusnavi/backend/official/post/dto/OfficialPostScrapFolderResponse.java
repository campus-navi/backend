package com.campusnavi.backend.official.post.dto;

public record OfficialPostScrapFolderResponse(
        Long folderId,
        String name,
        boolean isScrapped
) {
}
