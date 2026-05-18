package com.campusnavi.backend.member.dto;

import java.util.List;

public record MemberProfile(
        String nickname,
        String email,
        String campus,
        Integer admissionYear,
        Integer grade,
        List<String> departments
) {
}
