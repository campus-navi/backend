package com.campusnavi.backend.studio.academicplan.api;

import com.campusnavi.backend.studio.academicplan.MajorType;
import com.campusnavi.backend.studio.academicplan.api.dto.TargetCampusResponse;
import com.campusnavi.backend.studio.academicplan.api.dto.TargetDepartmentResponse;
import com.campusnavi.backend.studio.academicplan.api.dto.TargetMajorResponse;
import com.campusnavi.backend.studio.academicplan.service.AcademicPlanTargetService;
import com.campusnavi.backend.global.response.ApiResponse;
import com.campusnavi.backend.global.security.AuthMember;
import io.swagger.v3.oas.annotations.Operation;
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

@Tag(name = "12. [스튜디오] 학업계획서 대상 선택", description = "학업계획서 작성 대상 캠퍼스/학과/전공 선택 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/academic-plans/target")
public class AcademicPlanTargetController {

    private final AcademicPlanTargetService academicPlanTargetService;

    @Operation(summary = "캠퍼스 목록 조회", description = "소속 대학교 기준 캠퍼스 목록을 반환한다.")
    @GetMapping("/campuses")
    public ResponseEntity<ApiResponse<List<TargetCampusResponse>>> getCampuses(
            @AuthenticationPrincipal AuthMember authMember) {
        return ResponseEntity.ok(ApiResponse.ok(
                academicPlanTargetService.getCampuses(authMember.universityId())));
    }

    @Operation(summary = "대상 학과 목록 조회", description = "이중전공/복합전공 지원 가능 학과 목록을 반환한다. type은 DOUBLE_MAJOR, COMPLEX_MAJOR만 허용된다.")
    @GetMapping("/campuses/{campusId}/departments")
    public ResponseEntity<ApiResponse<List<TargetDepartmentResponse>>> getDepartments(
            @PathVariable Long campusId,
            @RequestParam MajorType type,
            @AuthenticationPrincipal AuthMember authMember) {
        return ResponseEntity.ok(ApiResponse.ok(
                academicPlanTargetService.getDepartments(campusId, type, authMember.memberId())));
    }

    @Operation(summary = "대상 전공 목록 조회", description = "융합전공/학생설계전공 목록을 반환한다. type은 CONVERGENCE_MAJOR, STUDENT_DESIGN만 허용된다.")
    @GetMapping("/campuses/{campusId}/majors")
    public ResponseEntity<ApiResponse<List<TargetMajorResponse>>> getMajors(
            @PathVariable Long campusId,
            @RequestParam MajorType type) {
        return ResponseEntity.ok(ApiResponse.ok(
                academicPlanTargetService.getMajors(campusId, type)));
    }
}
