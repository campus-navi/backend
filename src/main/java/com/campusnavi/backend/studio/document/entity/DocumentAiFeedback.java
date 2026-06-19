package com.campusnavi.backend.studio.document.entity;

import com.campusnavi.backend.global.common.BaseCreatedAtEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "document_ai_feedback")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DocumentAiFeedback extends BaseCreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    private DocumentSection section;

    @Column(nullable = false)
    private int orderNum;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String sentence;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String originalText;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String suggestion;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;
}
