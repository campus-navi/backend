package com.campusnavi.backend.scrap.dto;

import com.campusnavi.backend.scrap.entity.ScrapFolder;

public record ScrapFolderResponse(
        Long folderId,
        String name,
        String description,
        long scrapCount
) {
    public static ScrapFolderResponse of(ScrapFolder folder) {
        return new ScrapFolderResponse(
                folder.getId(), folder.getName(), folder.getDescription(), folder.getScrapCount());
    }
}
