package com.campusnavi.backend.studio.document.entity;

import java.util.List;
import java.util.Optional;

public enum DocumentType {
    // SectionSpec.required는 analyze 단계에서 필수 섹션 작성 여부 검증에 사용 예정 (이 PR 미사용)
    ACADEMIC_PLAN(List.of(
            new SectionSpec("application_motive", 1000, true, 1),
            new SectionSpec("interest_field", 1000, true, 2),
            new SectionSpec("study_plan", 1000, true, 3),
            new SectionSpec("academic_plan_etc", 1000, true, 4)
    ));

    private final List<SectionSpec> sections;

    DocumentType(List<SectionSpec> sections) {
        this.sections = sections;
    }

    public Optional<SectionSpec> findSection(String key) {
        return sections.stream()
                .filter(section -> section.key().equals(key))
                .findFirst();
    }
}
