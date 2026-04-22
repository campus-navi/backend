package com.campusnavi.backend.member.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record MemberInterestUpdateRequest(
        @NotNull
        List<Long> interestIds
) {
}
