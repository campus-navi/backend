package com.campusnavi.backend.university.controller;

import com.campusnavi.backend.global.response.ApiResponse;
import com.campusnavi.backend.global.response.ErrorResponse;
import com.campusnavi.backend.university.dto.CampusSummaryResponse;
import com.campusnavi.backend.university.dto.DepartmentSummaryResponse;
import com.campusnavi.backend.university.service.CampusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "캠퍼스", description = "캠퍼스 및 학과 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/campuses")
public class CampusController {

    private final CampusService campusService;

    @Operation(summary = "캠퍼스 목록 조회")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<CampusSummaryResponse>>> getCampusList() {
        return ResponseEntity.ok(ApiResponse.ok(campusService.getCampusList()));
    }

    @Operation(summary = "캠퍼스별 학과 목록 조회")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 캠퍼스",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{campusId}/departments")
    public ResponseEntity<ApiResponse<List<DepartmentSummaryResponse>>> getDepartmentList(@PathVariable Long campusId) {
        return ResponseEntity.ok(ApiResponse.ok(campusService.getDepartmentList(campusId)));
    }
}
