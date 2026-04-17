package com.campusnavi.backend.community.post.entity;

import com.campusnavi.backend.global.common.BaseEntity;
import com.campusnavi.backend.member.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long universityId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private boolean isAnonymous = true;

    @Column(nullable = false)
    private int likeCount = 0;

    @Column(nullable = false)
    private int scrapCount = 0;

    @Column(nullable = false)
    private int commentCount = 0;


    private LocalDateTime deletedAt;

    public static Post create(Long universityId, Member member, String title,
                              String content, boolean isAnonymous) {
        Post post = new Post();
        post.universityId = universityId;
        post.member = member;
        post.title = title;
        post.content = content;
        post.isAnonymous = isAnonymous;
        return post;
    }

    public void update(String title, String content, boolean isAnonymous) {
        this.title = title;
        this.content = content;
        this.isAnonymous = isAnonymous;
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    public void increaseCommentCount() {
        this.commentCount++;
    }

    public void decreaseCommentCount() {
        this.commentCount = Math.max(0, this.commentCount - 1);
    }
}
