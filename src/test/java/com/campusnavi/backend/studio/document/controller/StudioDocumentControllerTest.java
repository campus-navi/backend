package com.campusnavi.backend.studio.document.controller;

import com.campusnavi.backend.global.exception.BusinessException;
import com.campusnavi.backend.global.exception.ErrorCode;
import com.campusnavi.backend.global.security.AuthMember;
import com.campusnavi.backend.studio.document.controller.dto.DocumentDetailResponse;
import com.campusnavi.backend.studio.document.controller.dto.DocumentSummaryResponse;
import com.campusnavi.backend.studio.document.controller.dto.SectionResponse;
import com.campusnavi.backend.studio.document.entity.DocumentStatus;
import com.campusnavi.backend.studio.document.entity.DocumentType;
import com.campusnavi.backend.studio.document.service.StudioDocumentService;
import com.campusnavi.backend.support.ControllerSliceTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ControllerSliceTest(controllers = StudioDocumentController.class)
class StudioDocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

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
                    Map.of("majorType", "DOUBLE_MAJOR", "targetName", "경제학과"), LocalDateTime.now());
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
}
