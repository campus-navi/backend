package com.campusnavi.backend.scrap.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ScrapBulkDeleteRequest(
        @NotEmpty
        List<@NotNull Long> scrapIds
) {
}
