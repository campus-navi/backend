package com.campusnavi.backend.global.security;

public record AuthMember(
        Long memberId,
        String role
) {
}
