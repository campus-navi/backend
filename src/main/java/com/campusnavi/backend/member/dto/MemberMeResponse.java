package com.campusnavi.backend.member.dto;

public record MemberMeResponse(
        String nickname,
        boolean hasSetInterests
) {
}
