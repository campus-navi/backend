package com.campusnavi.backend.studio.academicplan.controller;

import com.campusnavi.backend.global.exception.ErrorCode;
import com.campusnavi.backend.global.security.AuthMember;
import com.campusnavi.backend.studio.academicplan.dto.DocumentCreateRequest;
import com.campusnavi.backend.studio.academicplan.dto.SectionInput;
import com.campusnavi.backend.studio.academicplan.service.AcademicPlanDocumentService;
import com.campusnavi.backend.studio.academicplan.entity.MajorType;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ControllerSliceTest(controllers = AcademicPlanDocumentController.class)
class AcademicPlanDocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AcademicPlanDocumentService academicPlanDocumentService;

    private static final Long MEMBER_ID = 1L;
    private static final Authentication AUTH = new UsernamePasswordAuthenticationToken(
            new AuthMember(MEMBER_ID, "USER", 10L), null, List.of()
    );

    @Nested
    @DisplayName("학업계획서 생성")
    class Create {

        @Test
        @DisplayName("정상 요청이면 201을 반환한다")
        void success() throws Exception {
            DocumentCreateRequest request = new DocumentCreateRequest(
                    MajorType.DOUBLE_MAJOR, 5L, List.of(new SectionInput("application_motive", "내용")));
            willDoNothing().given(academicPlanDocumentService).create(eq(MEMBER_ID), any());

            mockMvc.perform(post("/api/v1/academic-plans/documents").with(authentication(AUTH))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("섹션이 비어 있으면 400을 반환한다")
        void emptySections() throws Exception {
            DocumentCreateRequest request = new DocumentCreateRequest(MajorType.DOUBLE_MAJOR, 5L, List.of());

            mockMvc.perform(post("/api/v1/academic-plans/documents").with(authentication(AUTH))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.name()));
        }

        @Test
        @DisplayName("majorType이 없으면 400을 반환한다")
        void missingMajorType() throws Exception {
            DocumentCreateRequest request = new DocumentCreateRequest(
                    null, 5L, List.of(new SectionInput("application_motive", "내용")));

            mockMvc.perform(post("/api/v1/academic-plans/documents").with(authentication(AUTH))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.name()));
        }

        @Test
        @DisplayName("content가 비면 400을 반환한다")
        void blankContent() throws Exception {
            DocumentCreateRequest request = new DocumentCreateRequest(
                    MajorType.DOUBLE_MAJOR, 5L, List.of(new SectionInput("application_motive", " ")));

            mockMvc.perform(post("/api/v1/academic-plans/documents").with(authentication(AUTH))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.name()));
        }
    }
}
