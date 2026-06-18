package com.campusnavi.backend.mypage.dto;

import java.util.List;

public record MyPageResponse(
        String name,
        String nickname,
        String email,
        String campus,
        String studentNumber,
        Integer admissionYear,
        Integer grade,
        List<String> departments,
        long scrapCount,
        long remindCount,
        long interestCount
) {
}
