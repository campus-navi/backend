package com.campusnavi.backend.university.controller;

import com.campusnavi.backend.global.exception.BusinessException;
import com.campusnavi.backend.global.exception.ErrorCode;
import com.campusnavi.backend.support.ControllerSliceTest;
import com.campusnavi.backend.university.dto.CampusSummaryResponse;
import com.campusnavi.backend.university.dto.DepartmentSummaryResponse;
import com.campusnavi.backend.university.service.CampusService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ControllerSliceTest(controllers = CampusController.class)
class CampusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CampusService campusService;

    @Test
    @DisplayName("캠퍼스 조회에 성공하면 DTO와 함께 200반환")
    void getCampusListSuccess() throws Exception {
        //given
        CampusSummaryResponse response = new CampusSummaryResponse(1L,"서울캠퍼스");
        given(campusService.getCampusList()).willReturn(List.of(response));

        //when, then
        mockMvc.perform(get("/api/v1/campuses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("서울캠퍼스"));
    }

    @Test
    @DisplayName("학과 조회에 성공하면 DTO와 함께 200반환")
    void getDepartmentListSuccess() throws Exception{
        //given
        DepartmentSummaryResponse response = new DepartmentSummaryResponse(1L, "컴퓨터공학과");
        given(campusService.getDepartmentList(1L)).willReturn(List.of(response));

        //when, then
        mockMvc.perform(get("/api/v1/campuses/1/departments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("컴퓨터공학과"));
    }

    @Test
    @DisplayName("잘못된 형식의 CampusId를 보내면 400반환")
    void getDepartmentListInvalidParam() throws Exception {
        mockMvc.perform(get("/api/v1/campuses/abc/departments"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_PARAM.name()));
    }

    @Test
    @DisplayName("존재하지 않는 CampusId를 보내면 404반환")
    void getDepartmentListNotFoundByCampusId() throws Exception {
        //given
        Long campusId = 99L;
        given(campusService.getDepartmentList(campusId))
                .willThrow(new BusinessException(ErrorCode.CAMPUS_NOT_FOUND));

        //when, then
        mockMvc.perform(get("/api/v1/campuses/{campusId}/departments", campusId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ErrorCode.CAMPUS_NOT_FOUND.name()));
    }
}
