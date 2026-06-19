package com.campusnavi.backend.studio.document.entity;

import com.campusnavi.backend.global.common.BaseEntity;
import com.campusnavi.backend.member.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(name = "studio_document")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudioDocument extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private DocumentType documentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DocumentStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "JSONB")
    private Object metadata;

    public static StudioDocument create(Member member, DocumentType documentType, Object metadata) {
        StudioDocument doc = new StudioDocument();
        doc.member = member;
        doc.documentType = documentType;
        doc.status = DocumentStatus.DRAFT;
        doc.metadata = metadata;
        return doc;
    }
}
