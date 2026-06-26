package com.campusnavi.backend.studio.document.dto;

import com.campusnavi.backend.studio.document.entity.SectionSpec;

public record ResolvedSection(
        SectionSpec spec,
        String content
) {
}
