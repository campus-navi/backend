package com.campusnavi.backend.studio.document.entity;

import com.campusnavi.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "document_section",
        uniqueConstraints = @UniqueConstraint(name = "uq_ds_document_section",
                columnNames = {"document_id", "section_key"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DocumentSection extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private StudioDocument document;

    @Column(nullable = false, length = 50)
    private String sectionKey;

    @Column(nullable = false)
    private int sortOrder;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    public static DocumentSection create(StudioDocument document, String sectionKey, int sortOrder, String content) {
        DocumentSection section = new DocumentSection();
        section.document = document;
        section.sectionKey = sectionKey;
        section.sortOrder = sortOrder;
        section.content = content;
        return section;
    }
}
