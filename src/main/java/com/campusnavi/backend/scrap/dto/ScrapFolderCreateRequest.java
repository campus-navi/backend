package com.campusnavi.backend.scrap.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ScrapFolderCreateRequest(
        @NotBlank
        @Size(max = 20)
        String name,

        @Size(max = 20)
        String description
) {
}
