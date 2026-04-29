package com.campusnavi.backend.feed.controller;

import com.campusnavi.backend.feed.dto.CardListResponse;
import com.campusnavi.backend.feed.dto.DeadlineListResponse;
import com.campusnavi.backend.feed.service.FeedService;
import com.campusnavi.backend.global.common.AuthContext;
import com.campusnavi.backend.global.response.ApiResponse;
import com.campusnavi.backend.global.response.ErrorResponse;
import com.campusnavi.backend.global.security.AuthMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "8. 피드", description = "메인화면 카드뉴스 섹션·마감임박 공지 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/feed")
public class FeedController {

    private final FeedService feedService;

    @Operation(summary = "카드뉴스 조회", description = "최신 공지 9건과 관심사 기반 추천 공지 8건을 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/cards")
    public ResponseEntity<ApiResponse<CardListResponse>> getFeedCards(
            @AuthenticationPrincipal AuthMember authMember) {
        return ResponseEntity.ok(
                ApiResponse.ok(feedService.getCardLists(AuthContext.of(authMember))));
    }

    @Operation(summary = "마감임박 공지 미리보기", description = "7일 이내 마감 공지를 최대 8건 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/deadlines/preview")
    public ResponseEntity<ApiResponse<DeadlineListResponse>> getDeadlinePostsForFeed(
            @AuthenticationPrincipal AuthMember authMember) {
        return ResponseEntity.ok(
                ApiResponse.ok(feedService.getDeadlinePostsForFeed(AuthContext.of(authMember))));
    }

    @Operation(summary = "마감임박 공지 전체 조회", description = "7일 이내 마감 공지 전체를 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/deadlines")
    public ResponseEntity<ApiResponse<DeadlineListResponse>> getAllDeadlinePosts(
            @AuthenticationPrincipal AuthMember authMember) {
        return ResponseEntity.ok(
                ApiResponse.ok(feedService.getAllDeadlinePosts(AuthContext.of(authMember))));
    }
}
