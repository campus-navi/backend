package com.campusnavi.backend.mypage.controller;

import com.campusnavi.backend.global.response.ApiResponse;
import com.campusnavi.backend.global.response.CursorPageResponse;
import com.campusnavi.backend.global.security.AuthMember;
import com.campusnavi.backend.mypage.dto.MyPageResponse;
import com.campusnavi.backend.mypage.dto.MyScrapResponse;
import com.campusnavi.backend.official.post.dto.RecentViewResponse;
import com.campusnavi.backend.mypage.service.MyPageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "마이페이지", description = "마이페이지 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/mypage")
public class MyPageController {

    private final MyPageService myPageService;

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
                myPageService.getMyScraps(authMember.memberId())));
    }

    @Operation(summary = "최근 본 게시물 조회",
            description = "최근 본 게시물을 lastViewedAt 내림차순 커서 페이지로 반환한다. 단일커서로 인코딩 없는 ISO LocalDateTime 문자열")
    @GetMapping("/recent-views")
    public ResponseEntity<ApiResponse<CursorPageResponse<RecentViewResponse>>> getRecentViews(
            @AuthenticationPrincipal AuthMember authMember,
            @RequestParam(required = false) String cursor) {
        return ResponseEntity.ok(ApiResponse.ok(
                myPageService.getRecentViews(authMember.memberId(), cursor)));
    }

    @Operation(summary = "최근 본 게시물 단건 삭제",
            description = "본인의 최근 본 게시물 기록을 단건 제거한다. 존재하지 않아도 멱등하게 204를 반환한다.")
    @DeleteMapping("/recent-views/{postId}")
    public ResponseEntity<Void> deleteRecentView(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long postId) {
        myPageService.deleteRecentView(authMember.memberId(), postId);
        return ResponseEntity.noContent().build();
    }
}
