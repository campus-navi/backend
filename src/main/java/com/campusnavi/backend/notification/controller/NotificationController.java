package com.campusnavi.backend.notification.controller;

import com.campusnavi.backend.global.common.AuthContext;
import com.campusnavi.backend.global.response.ApiResponse;
import com.campusnavi.backend.global.response.ErrorResponse;
import com.campusnavi.backend.global.security.AuthMember;
import com.campusnavi.backend.notification.dto.MissedNotice;
import com.campusnavi.backend.notification.dto.MissedNoticeCard;
import com.campusnavi.backend.notification.dto.RemindNotice;
import com.campusnavi.backend.notification.service.ActivityNotificationService;
import com.campusnavi.backend.notification.service.RemindNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "10. 알림", description = "알림 - 활동/리마인드 알림 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final ActivityNotificationService activityNotificationService;
    private final RemindNotificationService remindNotificationService;

    @Operation(summary = "지나친 추천 공지 목록 조회",
            description = "최근 보관 기간(13일) 동안의 추천 공지를 missedDate(어제 09:00 ~ 오늘 09:00) 단위로 묶어 카드 목록으로 반환합니다. " +
                    "count는 미열람 개수이며, 모두 열람한 missedDate도 count=0 카드로 함께 반환됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/activity")
    public ResponseEntity<ApiResponse<List<MissedNoticeCard>>> getActivityCards(
            @AuthenticationPrincipal AuthMember authMember) {
        return ResponseEntity.ok(ApiResponse.ok(
                activityNotificationService.getActivityCards(AuthContext.of(authMember))));
    }

    @Operation(summary = "지나친 공지 상세 조회",
            description = "특정 missedDate의 미열람 공지 목록을 반환합니다. snapshot.postIds 원본 순서를 유지하며, 응답 시점 view 상태로 미열람 항목만 차감됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "해당 missedDate 스냅샷 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/activity/{missedDate}")
    public ResponseEntity<ApiResponse<List<MissedNotice>>> getActivityDetail(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate missedDate,
            @AuthenticationPrincipal AuthMember authMember) {
        return ResponseEntity.ok(ApiResponse.ok(
                activityNotificationService.getActivityDetail(AuthContext.of(authMember), missedDate)));
    }

    @Operation(summary = "리마인드 공지 목록 조회",
            description = "알림 설정한 공지 중 마감기한이 있고 아직 지나지 않은 공지를 마감 임박 순(endDate 오름차순)으로 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/remind")
    public ResponseEntity<ApiResponse<List<RemindNotice>>> getRemindNotices(
            @AuthenticationPrincipal AuthMember authMember) {
        return ResponseEntity.ok(ApiResponse.ok(
                remindNotificationService.getRemindNotices(AuthContext.of(authMember))));
    }
}
