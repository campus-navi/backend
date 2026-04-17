package com.campusnavi.backend.community.post.entity;

import com.campusnavi.backend.global.common.BaseCreatedAtEntity;
import com.campusnavi.backend.member.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"member_id", "post_id"}))
public class PostLike extends BaseCreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    public static PostLike create(Member member, Post post) {
        PostLike like = new PostLike();
        like.member = member;
        like.post = post;
        return like;
    }
}
