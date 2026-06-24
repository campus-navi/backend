package com.campusnavi.backend.community.post.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostImage{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(nullable = false, columnDefinition = "TEXT", name = "image_key")
    private String imageKey;

    @Column(nullable = false)
    private int sortOrder = 0;

    public static PostImage create(Post post, String imageKey, int sortOrder) {
        PostImage image = new PostImage();
        image.post = post;
        image.imageKey = imageKey;
        image.sortOrder = sortOrder;
        return image;
    }
}