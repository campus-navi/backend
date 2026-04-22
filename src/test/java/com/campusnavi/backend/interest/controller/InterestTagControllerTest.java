package com.campusnavi.backend.interest.controller;

import com.campusnavi.backend.interest.dto.InterestTagResponse;
import com.campusnavi.backend.interest.service.InterestTagService;
import com.campusnavi.backend.support.ControllerSliceTest;
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

@ControllerSliceTest(controllers = InterestTagController.class)
class InterestTagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InterestTagService interestTagService;

    @Test
    @DisplayName("관심사 목록 조회에 성공하면 200과 태그 목록을 반환한다")
    void getInterestTags_success() throws Exception {
        // given
        List<InterestTagResponse> tags = List.of(
                new InterestTagResponse(1L, "장학금"),
                new InterestTagResponse(2L, "취업·채용")
        );
        given(interestTagService.getInterestTags()).willReturn(tags);

        // when & then
        mockMvc.perform(get("/api/v1/interests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1L))
                .andExpect(jsonPath("$.data[0].name").value("장학금"))
                .andExpect(jsonPath("$.data[1].name").value("취업·채용"));
    }

    @Test
    @DisplayName("추천 가능한 태그가 없으면 200과 빈 목록을 반환한다")
    void getInterestTags_empty() throws Exception {
        // given
        given(interestTagService.getInterestTags()).willReturn(List.of());

        // when & then
        mockMvc.perform(get("/api/v1/interests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isEmpty());
    }
}
