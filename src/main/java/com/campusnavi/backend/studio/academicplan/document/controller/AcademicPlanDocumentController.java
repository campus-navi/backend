package com.campusnavi.backend.studio.academicplan.document.controller;

import com.campusnavi.backend.global.response.ApiResponse;
import com.campusnavi.backend.global.security.AuthMember;
import com.campusnavi.backend.studio.academicplan.document.dto.DocumentCreateRequest;
import com.campusnavi.backend.studio.academicplan.document.service.AcademicPlanDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "13. [스튜디오] 학업계획서 작성", description = "학업계획서 문서 생성 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/academic-plans/documents")
public class AcademicPlanDocumentController {

    private final AcademicPlanDocumentService academicPlanDocumentService;

    @Operation(summary = "학업계획서 생성",
            description = "지원대상(majorType + targetId)과 작성된 섹션으로 학업계획서를 임시저장(DRAFT)합니다. 섹션은 1개 이상이어야 합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> create(
            @RequestBody @Valid DocumentCreateRequest request,
            @AuthenticationPrincipal AuthMember authMember) {
        academicPlanDocumentService.create(authMember.memberId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok());
    }
}
