package com.campusnavi.backend.scrap.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ScrapFolderUpdateRequest(
        @NotBlank
        @Size(max = 20)
        String name,

        @Size(max = 20)
        String description
) {
}
