package com.campusnavi.backend.official.entity;

import com.campusnavi.backend.global.common.BaseCreatedAtEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OfficialAttachment extends BaseCreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private OfficialPost post;

    @Column(nullable = false, length = 500)
    private String originalName;

    @Column(nullable = false, length = 500)
    private String s3Key;

    @Column(nullable = false, length = 100)
    private String contentType;

    @Column(nullable = false)
    private boolean isImage;

    @Column(nullable = false)
    private short sortOrder;

    public static OfficialAttachment create(OfficialPost post, String originalName, String s3Key,
                                            String contentType, boolean isImage, short sortOrder) {
        OfficialAttachment attachment = new OfficialAttachment();
        attachment.post = post;
        attachment.originalName = originalName;
        attachment.s3Key = s3Key;
        attachment.contentType = contentType;
        attachment.isImage = isImage;
        attachment.sortOrder = sortOrder;
        return attachment;
    }
}