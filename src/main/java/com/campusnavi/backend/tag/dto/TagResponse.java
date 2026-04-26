package com.campusnavi.backend.tag.dto;

import com.campusnavi.backend.tag.entity.Tag;

public record TagResponse(
        Long id,
        String name
) {
    public static TagResponse of(Tag tag) {
        return new TagResponse(tag.getId(), tag.getName());
    }
}
