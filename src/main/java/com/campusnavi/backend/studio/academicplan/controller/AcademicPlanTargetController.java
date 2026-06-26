package com.campusnavi.backend.studio.academicplan.controller;

import com.campusnavi.backend.studio.academicplan.entity.MajorType;
import com.campusnavi.backend.studio.academicplan.controller.dto.TargetCampusResponse;
import com.campusnavi.backend.studio.academicplan.controller.dto.TargetDepartmentResponse;
import com.campusnavi.backend.studio.academicplan.controller.dto.TargetMajorResponse;
import com.campusnavi.backend.studio.academicplan.service.AcademicPlanTargetService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "12. [스튜디오] 학업계획서", description = "학업계획서 작성 대상(캠퍼스/학과/전공) 선택 및 문서 생성 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/academic-plans/target")
public class AcademicPlanTargetController {

    private final AcademicPlanTargetService academicPlanTargetService;

    @Operation(summary = "캠퍼스 목록 조회", description = "소속 대학교 기준 캠퍼스 목록을 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/campuses")
    public ResponseEntity<ApiResponse<List<TargetCampusResponse>>> getCampuses(
            @AuthenticationPrincipal AuthMember authMember) {
        return ResponseEntity.ok(ApiResponse.ok(
                academicPlanTargetService.getCampuses(authMember.universityId())));
    }

    @Operation(summary = "대상 학과 목록 조회", description = "본인 소속 학과 기준 이중전공/복합전공 지원 가능 학과 목록을 반환합니다. type은 DOUBLE_MAJOR(이중전공), COMPLEX_MAJOR(복합전공)만 허용됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "허용되지 않은 type",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/campuses/{campusId}/departments")
    public ResponseEntity<ApiResponse<List<TargetDepartmentResponse>>> getDepartments(
            @PathVariable Long campusId,
            @RequestParam MajorType type,
            @AuthenticationPrincipal AuthMember authMember) {
        return ResponseEntity.ok(ApiResponse.ok(
                academicPlanTargetService.getDepartments(campusId, type, authMember.memberId())));
    }

    @Operation(summary = "대상 전공 목록 조회", description = "본인 소속 학과 기준 융합전공/학생설계전공 지원 가능 목록을 반환합니다. type은 CONVERGENCE_MAJOR(융합전공), STUDENT_DESIGN(학생설계전공)만 허용됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "허용되지 않은 type",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/campuses/{campusId}/majors")
    public ResponseEntity<ApiResponse<List<TargetMajorResponse>>> getMajors(
            @PathVariable Long campusId,
            @RequestParam MajorType type) {
        return ResponseEntity.ok(ApiResponse.ok(
                academicPlanTargetService.getMajors(campusId, type)));
    }
}
