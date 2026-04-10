package com.campusnavi.backend.university.controller;

import com.campusnavi.backend.global.exception.BusinessException;
import com.campusnavi.backend.global.exception.ErrorCode;
import com.campusnavi.backend.support.ControllerSliceTest;
import com.campusnavi.backend.university.service.CampusService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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
    @WithMockUser
    void getDepartmentList_잘못된PathVariable타입_400반환() throws Exception {
        mockMvc.perform(get("/api/v1/campuses/abc/departments"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_PARAM.getCode()));
    }

    @Test
    @WithMockUser
    void getDepartmentList_존재하지않는캠퍼스ID_404반환() throws Exception {
        Long campusId = 99L;
        given(campusService.getDepartmentList(campusId))
                .willThrow(new BusinessException(ErrorCode.CAMPUS_NOT_FOUND));

        mockMvc.perform(get("/api/v1/campuses/{campusId}/departments", campusId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ErrorCode.CAMPUS_NOT_FOUND.getCode()));
    }
}
