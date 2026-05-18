package com.campusnavi.backend.official.post.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record OfficialPostScrapRequest(
        @NotNull
        List<@NotNull Long> folderIds
) {
}
