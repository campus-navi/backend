package com.campusnavi.backend.official.post.entity;

import com.campusnavi.backend.global.common.BaseCreatedAtEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OfficialAttachmentDownload extends BaseCreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "attachment_id", nullable = false)
    private Long attachmentId;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    public static OfficialAttachmentDownload create(Long memberId, Long attachmentId, Long postId) {
        OfficialAttachmentDownload download = new OfficialAttachmentDownload();
        download.memberId = memberId;
        download.attachmentId = attachmentId;
        download.postId = postId;
        return download;
    }
}
