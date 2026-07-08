package com.campusnavi.backend.studio.document.controller;

import com.campusnavi.backend.global.exception.BusinessException;
import com.campusnavi.backend.global.exception.ErrorCode;
import com.campusnavi.backend.global.security.AuthMember;
import com.campusnavi.backend.studio.academicplan.entity.AcademicPlanMetadata;
import com.campusnavi.backend.studio.academicplan.entity.MajorType;
import com.campusnavi.backend.studio.document.dto.DocumentDetailResponse;
import com.campusnavi.backend.studio.document.dto.DocumentSummaryResponse;
import com.campusnavi.backend.studio.document.dto.DocumentUpdateRequest;
import com.campusnavi.backend.studio.document.dto.SectionResponse;
import com.campusnavi.backend.studio.document.dto.UpdateSectionInput;
import com.campusnavi.backend.studio.document.entity.DocumentStatus;
import com.campusnavi.backend.studio.document.entity.DocumentType;
import com.campusnavi.backend.studio.document.service.StudioDocumentService;
import com.campusnavi.backend.support.ControllerSliceTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ControllerSliceTest(controllers = StudioDocumentController.class)
class StudioDocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private StudioDocumentService studioDocumentService;

    private static final Long MEMBER_ID = 1L;
    private static final Long DOCUMENT_ID = 10L;
    private static final Authentication AUTH = new UsernamePasswordAuthenticationToken(
            new AuthMember(MEMBER_ID, "USER", 10L), null, List.of()
    );

    @Nested
    @DisplayName("내 문서함 목록 조회")
    class GetDocuments {

        @Test
        @DisplayName("정상 요청이면 200과 목록을 반환한다")
        void success() throws Exception {
            DocumentSummaryResponse summary = new DocumentSummaryResponse(
                    DOCUMENT_ID, DocumentType.ACADEMIC_PLAN, DocumentStatus.DRAFT,
                    new AcademicPlanMetadata(MajorType.DOUBLE_MAJOR, "서울캠퍼스", "경제학과"), LocalDateTime.now());
            given(studioDocumentService.getDocuments(MEMBER_ID)).willReturn(List.of(summary));

            mockMvc.perform(get("/api/v1/studio/documents").with(authentication(AUTH)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data[0].id").value(DOCUMENT_ID))
                    .andExpect(jsonPath("$.data[0].documentType").value("ACADEMIC_PLAN"))
                    .andExpect(jsonPath("$.data[0].metadata.targetName").value("경제학과"));
        }
    }

    @Nested
    @DisplayName("문서 원문 섹션 조회")
    class GetDocumentSections {

        @Test
        @DisplayName("정상 요청이면 200과 섹션을 반환한다")
        void success() throws Exception {
            DocumentDetailResponse detail = new DocumentDetailResponse(
                    DOCUMENT_ID, DocumentType.ACADEMIC_PLAN, DocumentStatus.DRAFT,
                    List.of(new SectionResponse("application_motive", "내용", 1)));
            given(studioDocumentService.getDocumentSections(eq(MEMBER_ID), eq(DOCUMENT_ID))).willReturn(detail);

            mockMvc.perform(get("/api/v1/studio/documents/{documentId}/sections", DOCUMENT_ID).with(authentication(AUTH)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(DOCUMENT_ID))
                    .andExpect(jsonPath("$.data.sections[0].sectionKey").value("application_motive"))
                    .andExpect(jsonPath("$.data.sections[0].content").value("내용"));
        }

        @Test
        @DisplayName("본인 문서가 아니면 404를 반환한다")
        void notFound() throws Exception {
            given(studioDocumentService.getDocumentSections(eq(MEMBER_ID), eq(DOCUMENT_ID)))
                    .willThrow(new BusinessException(ErrorCode.STUDIO_DOCUMENT_NOT_FOUND));

            mockMvc.perform(get("/api/v1/studio/documents/{documentId}/sections", DOCUMENT_ID).with(authentication(AUTH)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(ErrorCode.STUDIO_DOCUMENT_NOT_FOUND.name()));
        }
    }

    @Nested
    @DisplayName("섹션 이어쓰기")
    class UpdateSections {

        @Test
        @DisplayName("정상 요청이면 200을 반환한다")
        void success() throws Exception {
            DocumentUpdateRequest request = new DocumentUpdateRequest(
                    List.of(new UpdateSectionInput("study_plan", "내용")));
            willDoNothing().given(studioDocumentService).updateSections(eq(MEMBER_ID), eq(DOCUMENT_ID), any());

            mockMvc.perform(patch("/api/v1/studio/documents/{documentId}", DOCUMENT_ID).with(authentication(AUTH))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("content가 비면 400을 반환한다")
        void blankContent() throws Exception {
            DocumentUpdateRequest request = new DocumentUpdateRequest(
                    List.of(new UpdateSectionInput("study_plan", " ")));

            mockMvc.perform(patch("/api/v1/studio/documents/{documentId}", DOCUMENT_ID).with(authentication(AUTH))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.name()));
        }

        @Test
        @DisplayName("본인 문서가 아니면 404를 반환한다")
        void notFound() throws Exception {
            DocumentUpdateRequest request = new DocumentUpdateRequest(
                    List.of(new UpdateSectionInput("study_plan", "내용")));
            willThrow(new BusinessException(ErrorCode.STUDIO_DOCUMENT_NOT_FOUND))
                    .given(studioDocumentService).updateSections(eq(MEMBER_ID), eq(DOCUMENT_ID), any());

            mockMvc.perform(patch("/api/v1/studio/documents/{documentId}", DOCUMENT_ID).with(authentication(AUTH))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(ErrorCode.STUDIO_DOCUMENT_NOT_FOUND.name()));
        }
    }
}
