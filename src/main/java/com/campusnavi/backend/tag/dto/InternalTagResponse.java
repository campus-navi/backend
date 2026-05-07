package com.campusnavi.backend.tag.dto;

import com.campusnavi.backend.tag.entity.Tag;

public record InternalTagResponse(
        String code,
        String name
) {
    public static InternalTagResponse of(Tag tag) {
        return new InternalTagResponse(tag.getCode(), tag.getName());
    }
}
