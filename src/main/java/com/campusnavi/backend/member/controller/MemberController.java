package com.campusnavi.backend.member.controller;

import com.campusnavi.backend.global.response.ApiResponse;
import com.campusnavi.backend.global.security.AuthMember;
import com.campusnavi.backend.member.dto.MemberInterestUpdateRequest;
import com.campusnavi.backend.member.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "7. 멤버", description = "멤버 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/members")
public class MemberController {

    private final MemberService memberService;

    @Operation(summary = "관심사 전체 교체/등록", description = "멤버의 관심사를 전달된 목록으로 전체 교체/등록한다. 빈 배열 전달 시 전체 해제.")
    @SecurityRequirement(name = "Authorization")
    @PutMapping("/me/interests")
    public ResponseEntity<ApiResponse<Void>> updateInterests(
            @AuthenticationPrincipal AuthMember authMember,
            @RequestBody @Valid MemberInterestUpdateRequest request) {
        memberService.updateMemberInterests(authMember, request);
        return ResponseEntity.ok(ApiResponse.ok());
    }

}
