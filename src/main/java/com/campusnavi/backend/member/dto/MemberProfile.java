package com.campusnavi.backend.member.dto;

import java.util.List;

public record MemberProfile(
        String name,
        String nickname,
        String email,
        String campus,
        String studentNumber,
        Integer admissionYear,
        Integer grade,
        List<String> departments
) {
}
