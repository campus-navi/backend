package com.campusnavi.backend.member.controller;

import com.campusnavi.backend.global.response.ApiResponse;
import com.campusnavi.backend.global.security.AuthMember;
import com.campusnavi.backend.member.dto.AdmissionYearUpdateRequest;
import com.campusnavi.backend.member.dto.GradeUpdateRequest;
import com.campusnavi.backend.member.dto.MemberInterestUpdateRequest;
import com.campusnavi.backend.member.dto.MemberMeResponse;
import com.campusnavi.backend.member.dto.PasswordUpdateRequest;
import com.campusnavi.backend.member.dto.UsernameUpdateRequest;
import com.campusnavi.backend.member.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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

    @Operation(summary = "아이디 변경", description = "아이디를 변경한다. 중복된 아이디면 409를 반환한다.")
    @PatchMapping("/me/username")
    public ResponseEntity<ApiResponse<Void>> changeUsername(
            @AuthenticationPrincipal AuthMember authMember,
            @RequestBody @Valid UsernameUpdateRequest request) {
        memberService.changeUsername(authMember.memberId(), request);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(summary = "비밀번호 변경", description = "비밀번호를 변경한다.")
    @PatchMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal AuthMember authMember,
            @RequestBody @Valid PasswordUpdateRequest request) {
        memberService.changePassword(authMember.memberId(), request);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(summary = "학번 변경", description = "학번(입학년도)을 변경한다.")
    @PatchMapping("/me/admission-year")
    public ResponseEntity<ApiResponse<Void>> changeAdmissionYear(
            @AuthenticationPrincipal AuthMember authMember,
            @RequestBody @Valid AdmissionYearUpdateRequest request) {
        memberService.changeAdmissionYear(authMember.memberId(), request);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(summary = "학년 변경", description = "학년을 변경한다.")
    @PatchMapping("/me/grade")
    public ResponseEntity<ApiResponse<Void>> changeGrade(
            @AuthenticationPrincipal AuthMember authMember,
            @RequestBody @Valid GradeUpdateRequest request) {
        memberService.changeGrade(authMember.memberId(), request);
        return ResponseEntity.ok(ApiResponse.ok());
    }

}
