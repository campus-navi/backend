package com.campusnavi.backend.community.comment.entity;

import com.campusnavi.backend.community.post.entity.Post;
import com.campusnavi.backend.global.common.BaseCreatedAtEntity;
import com.campusnavi.backend.member.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends BaseCreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;

    @Column(nullable = false)
    private int replyCount = 0;

    @Column(nullable = false)
    private int likeCount = 0;

    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private boolean isAnonymous = true;

    private LocalDateTime deletedAt;

    public static Comment create(Post post, Member member, Comment parent, String content, boolean isAnonymous) {
        Comment comment = new Comment();
        comment.post = post;
        comment.member = member;
        comment.parent = parent;
        comment.content = content;
        comment.isAnonymous = isAnonymous;
        return comment;
    }

    public void update(String content, boolean isAnonymous) {
        this.content = content;
        this.isAnonymous = isAnonymous;
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }


}
