package com.campusnavi.backend.scrap.controller;

import com.campusnavi.backend.global.common.AuthContext;
import com.campusnavi.backend.global.response.ApiResponse;
import com.campusnavi.backend.global.response.ErrorResponse;
import com.campusnavi.backend.global.security.AuthMember;
import com.campusnavi.backend.official.post.dto.FolderScrapResponse;
import com.campusnavi.backend.official.post.service.OfficialPostScrapService;
import com.campusnavi.backend.scrap.dto.ScrapFolderCreateRequest;
import com.campusnavi.backend.scrap.dto.ScrapFolderResponse;
import com.campusnavi.backend.scrap.dto.ScrapFolderSort;
import com.campusnavi.backend.scrap.dto.ScrapFolderUpdateRequest;
import com.campusnavi.backend.scrap.service.ScrapFolderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "11. 스크랩 폴더", description = "스크랩 폴더 CRUD API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/scrap-folders")
public class ScrapFolderController {

    private final ScrapFolderService scrapFolderService;
    private final OfficialPostScrapService officialPostScrapService;

    @Operation(summary = "스크랩 폴더 생성", description = "회원 내 이름이 중복되면 409를 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이름 중복",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> create(
            @RequestBody @Valid ScrapFolderCreateRequest request,
            @AuthenticationPrincipal AuthMember authMember) {
        scrapFolderService.create(authMember.memberId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok());
    }

    @Operation(summary = "스크랩 폴더 수정", description = "이름·설명을 수정합니다. 본인 소유 폴더만 수정할 수 있습니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않거나 타인 소유 폴더",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이름 중복",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> update(
            @PathVariable Long id,
            @RequestBody @Valid ScrapFolderUpdateRequest request,
            @AuthenticationPrincipal AuthMember authMember) {
        scrapFolderService.update(authMember.memberId(), id, request);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(summary = "스크랩 폴더 삭제", description = "본인 소유 폴더만 삭제할 수 있습니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않거나 타인 소유 폴더",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthMember authMember) {
        scrapFolderService.delete(authMember.memberId(), id);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(summary = "스크랩 폴더 목록 조회",
            description = "본인 폴더를 정렬 조건으로 조회합니다. (RECENT_SAVED | NAME_ASC | NAME_DESC | LIST_ADDED)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<ScrapFolderResponse>>> getFolders(
            @Parameter(description = "정렬 (RECENT_SAVED | NAME_ASC | NAME_DESC | LIST_ADDED), 기본 RECENT_SAVED")
            @RequestParam(defaultValue = "RECENT_SAVED") ScrapFolderSort sort,
            @AuthenticationPrincipal AuthMember authMember) {
        return ResponseEntity.ok(ApiResponse.ok(
                scrapFolderService.getFolders(authMember.memberId(), sort)));
    }

    @Operation(summary = "폴더 스크랩 목록 조회",
            description = "본인 폴더의 스크랩 게시글 목록을 스크랩한 순서(최신순)로 조회합니다. 비활성 게시글도 isActive=false로 포함됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않거나 타인 소유 폴더",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}/scraps")
    public ResponseEntity<ApiResponse<List<FolderScrapResponse>>> getFolderScraps(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthMember authMember) {
        return ResponseEntity.ok(ApiResponse.ok(
                officialPostScrapService.getFolderScraps(id, AuthContext.of(authMember))));
    }
}
