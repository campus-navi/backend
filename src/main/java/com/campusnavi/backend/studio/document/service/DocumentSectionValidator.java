package com.campusnavi.backend.studio.document.service;

import com.campusnavi.backend.global.exception.BusinessException;
import com.campusnavi.backend.global.exception.ErrorCode;
import com.campusnavi.backend.studio.document.dto.ResolvedSection;
import com.campusnavi.backend.studio.document.dto.SectionContent;
import com.campusnavi.backend.studio.document.entity.DocumentType;
import com.campusnavi.backend.studio.document.entity.SectionSpec;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class DocumentSectionValidator {

    public List<ResolvedSection> resolve(DocumentType type, List<? extends SectionContent> sections) {
        Set<String> seenKeys = new HashSet<>();
        List<ResolvedSection> resolved = new ArrayList<>();
        for (SectionContent section : sections) {
            SectionSpec spec = type.findSection(section.sectionKey())
                    .orElseThrow(() -> new BusinessException(ErrorCode.STUDIO_SECTION_KEY_INVALID));
            if (section.content().length() > spec.maxLength()) {
                throw new BusinessException(ErrorCode.STUDIO_SECTION_TOO_LONG);
            }
            if (!seenKeys.add(section.sectionKey())) {
                throw new BusinessException(ErrorCode.STUDIO_SECTION_KEY_INVALID);
            }
            resolved.add(new ResolvedSection(spec, section.content()));
        }
        return resolved;
    }
}
