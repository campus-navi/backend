package com.campusnavi.backend.studio.document.controller.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record DocumentUpdateRequest(
        @NotEmpty @Valid List<UpdateSectionInput> sections
) {
}
