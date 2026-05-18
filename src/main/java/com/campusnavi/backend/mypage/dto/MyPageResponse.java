package com.campusnavi.backend.mypage.dto;

import java.util.List;

public record MyPageResponse(
        String nickname,
        String email,
        String campus,
        Integer admissionYear,
        Integer grade,
        List<String> departments,
        long scrapCount,
        long remindCount
) {
}
