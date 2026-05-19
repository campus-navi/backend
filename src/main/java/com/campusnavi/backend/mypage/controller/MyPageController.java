package com.campusnavi.backend.mypage.controller;

import com.campusnavi.backend.global.response.ApiResponse;
import com.campusnavi.backend.global.security.AuthMember;
import com.campusnavi.backend.mypage.dto.MyPageResponse;
import com.campusnavi.backend.mypage.dto.MyScrapResponse;
import com.campusnavi.backend.mypage.service.MyPageService;
import com.campusnavi.backend.mypage.service.MyScrapService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "마이페이지", description = "마이페이지 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/mypage")
public class MyPageController {

    private final MyPageService myPageService;
    private final MyScrapService myScrapService;

    @Operation(summary = "마이페이지 기본화면 조회",
            description = "프로필 정보와 스크랩 수, 활성 리마인드 수를 반환한다.")
    @GetMapping
    public ResponseEntity<ApiResponse<MyPageResponse>> getMyPage(
            @AuthenticationPrincipal AuthMember authMember) {
        return ResponseEntity.ok(ApiResponse.ok(myPageService.getMyPage(authMember.memberId())));
    }

    @Operation(summary = "마이페이지 스크랩 화면 조회",
            description = "최근 스크랩 최대 8건과 스크랩 폴더 목록(최근 저장순)을 조합해 반환한다.")
    @GetMapping("/scraps")
    public ResponseEntity<ApiResponse<MyScrapResponse>> getMyScraps(
            @AuthenticationPrincipal AuthMember authMember) {
        return ResponseEntity.ok(ApiResponse.ok(
                myScrapService.getMyScraps(authMember.memberId())));
    }
}
