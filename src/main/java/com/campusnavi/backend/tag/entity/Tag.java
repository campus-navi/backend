package com.campusnavi.backend.tag.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private boolean isRecommendable;

    public static Tag create(String code, String name, boolean isRecommendable) {
        Tag tag = new Tag();
        tag.code = code;
        tag.name = name;
        tag.isRecommendable = isRecommendable;
        return tag;
    }

}
