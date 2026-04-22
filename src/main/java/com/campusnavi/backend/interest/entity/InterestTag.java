package com.campusnavi.backend.interest.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterestTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private short sortOrder;

    @Column(nullable = false)
    private boolean isRecommendable;

    public static InterestTag create(String code, String name, short sortOrder, boolean isRecommendable) {
        InterestTag tag = new InterestTag();
        tag.code = code;
        tag.name = name;
        tag.sortOrder = sortOrder;
        tag.isRecommendable = isRecommendable;
        return tag;
    }

}
