package com.campusnavi.backend.interest.dto;

import com.campusnavi.backend.interest.entity.InterestTag;

public record InterestTagResponse(
        Long id,
        String name
) {
    public static InterestTagResponse of(InterestTag tag) {
        return new InterestTagResponse(tag.getId(), tag.getName());
    }
}
