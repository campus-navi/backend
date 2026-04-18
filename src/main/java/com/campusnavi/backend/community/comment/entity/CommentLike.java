package com.campusnavi.backend.community.comment.entity;

import com.campusnavi.backend.global.common.BaseCreatedAtEntity;
import com.campusnavi.backend.member.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"member_id", "comment_id"}))
public class CommentLike extends BaseCreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", nullable = false)
    private Comment comment;

    public static CommentLike create(Member member, Comment comment) {
        CommentLike commentLike = new CommentLike();
        commentLike.member = member;
        commentLike.comment = comment;
        return commentLike;
    }
}
