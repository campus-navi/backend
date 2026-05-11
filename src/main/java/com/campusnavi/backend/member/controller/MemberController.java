package com.campusnavi.backend.member.controller;

import com.campusnavi.backend.global.response.ApiResponse;
import com.campusnavi.backend.global.security.AuthMember;
import com.campusnavi.backend.member.dto.MemberInterestUpdateRequest;
import com.campusnavi.backend.member.dto.MemberMeResponse;
import com.campusnavi.backend.member.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
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

    @Operation(summary = "내 정보 조회", description = "닉네임, 맞춤공지 설정 여부를 반환한다.")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MemberMeResponse>> getMe(
            @AuthenticationPrincipal AuthMember authMember) {
        return ResponseEntity.ok(ApiResponse.ok(memberService.getMe(authMember)));
    }

    @Operation(summary = "관심사 전체 교체/등록", description = "멤버의 관심사를 전달된 목록으로 전체 교체/등록한다. 빈 배열 전달 시 전체 해제.")
    @PutMapping("/me/interests")
    public ResponseEntity<ApiResponse<Void>> updateInterests(
            @AuthenticationPrincipal AuthMember authMember,
            @RequestBody @Valid MemberInterestUpdateRequest request) {
        memberService.updateMemberInterests(authMember, request);
        return ResponseEntity.ok(ApiResponse.ok());
    }

}
