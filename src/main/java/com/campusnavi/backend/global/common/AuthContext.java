package com.campusnavi.backend.global.common;

import com.campusnavi.backend.global.security.AuthMember;

public record AuthContext(
        Long memberId,
        Long universityId
) {
    public static AuthContext of(AuthMember member) {
        return new AuthContext(member.memberId(), member.universityId());
    }
}
