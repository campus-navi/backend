package com.campusnavi.backend.official.post.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "official_post_view",
        uniqueConstraints = @UniqueConstraint(name = "uq_official_post_view", columnNames = {"member_id", "post_id"}))
public class OfficialPostView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "first_viewed_at", nullable = false)
    private LocalDateTime firstViewedAt;

    @Column(name = "last_viewed_at", nullable = false)
    private LocalDateTime lastViewedAt;

    @Column(name = "view_count", nullable = false)
    private int viewCount;
}
