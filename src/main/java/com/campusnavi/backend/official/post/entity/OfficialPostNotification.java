package com.campusnavi.backend.official.post.entity;

import com.campusnavi.backend.global.common.BaseCreatedAtEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "official_post_notification",
        uniqueConstraints = @UniqueConstraint(name = "uq_official_post_notification", columnNames = {"member_id", "post_id"}))
public class OfficialPostNotification extends BaseCreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private OfficialPost post;

    public static OfficialPostNotification create(Long memberId, OfficialPost post) {
        OfficialPostNotification notification = new OfficialPostNotification();
        notification.memberId = memberId;
        notification.post = post;
        return notification;
    }
}
