package com.campusnavi.backend.studio.academicplan.controller;

import com.campusnavi.backend.studio.academicplan.dto.TargetCampusResponse;
import com.campusnavi.backend.studio.academicplan.dto.TargetDepartmentResponse;
import com.campusnavi.backend.studio.academicplan.dto.TargetMajorResponse;
import com.campusnavi.backend.studio.academicplan.service.AcademicPlanTargetService;
import com.campusnavi.backend.global.exception.ErrorCode;
import com.campusnavi.backend.global.security.AuthMember;
import com.campusnavi.backend.support.ControllerSliceTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ControllerSliceTest(controllers = AcademicPlanTargetController.class)
class AcademicPlanTargetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AcademicPlanTargetService academicPlanTargetService;

    private static final Authentication AUTH = new UsernamePasswordAuthenticationToken(
            new AuthMember(1L, "USER", 10L), null, List.of()
    );

    @Nested
    @DisplayName("캠퍼스 목록 조회")
    class GetCampuses {

        @Test
        @DisplayName("정상 요청이면 200과 목록을 반환한다")
        void success() throws Exception {
            // given
            given(academicPlanTargetService.getCampuses(anyLong()))
                    .willReturn(List.of(
                            new TargetCampusResponse(1L, "서울캠퍼스"),
                            new TargetCampusResponse(2L, "세종캠퍼스")
                    ));

            // when & then
            mockMvc.perform(get("/api/v1/academic-plans/target/campuses")
                            .with(authentication(AUTH)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.length()").value(2));
        }
    }

    @Nested
    @DisplayName("대상 학과 목록 조회")
    class GetDepartments {

        @Test
        @DisplayName("DOUBLE_MAJOR 요청이면 200을 반환한다")
        void doubleMajor() throws Exception {
            // given
            given(academicPlanTargetService.getDepartments(anyLong(), any(), anyLong()))
                    .willReturn(List.of(new TargetDepartmentResponse(1L, "컴퓨터공학부")));

            // when & then
            mockMvc.perform(get("/api/v1/academic-plans/target/campuses/2/departments")
                            .param("type", "DOUBLE_MAJOR")
                            .with(authentication(AUTH)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("유효하지 않은 type이면 400을 반환한다")
        void invalidType() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/academic-plans/target/campuses/2/departments")
                            .param("type", "INVALID_TYPE")
                            .with(authentication(AUTH)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_PARAM.name()));
        }
    }

    @Nested
    @DisplayName("대상 전공 목록 조회")
    class GetMajors {

        @Test
        @DisplayName("CONVERGENCE_MAJOR 요청이면 200을 반환한다")
        void convergenceMajor() throws Exception {
            // given
            given(academicPlanTargetService.getMajors(anyLong(), any()))
                    .willReturn(List.of(new TargetMajorResponse(1L, "금융공학")));

            // when & then
            mockMvc.perform(get("/api/v1/academic-plans/target/campuses/1/majors")
                            .param("type", "CONVERGENCE_MAJOR")
                            .with(authentication(AUTH)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("유효하지 않은 type이면 400을 반환한다")
        void invalidType() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/academic-plans/target/campuses/1/majors")
                            .param("type", "INVALID_TYPE")
                            .with(authentication(AUTH)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_PARAM.name()));
        }
    }
}
