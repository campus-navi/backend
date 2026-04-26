package com.campusnavi.backend.tag.controller;

import com.campusnavi.backend.tag.dto.TagResponse;
import com.campusnavi.backend.tag.service.TagService;
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

@ControllerSliceTest(controllers = TagController.class)
class TagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TagService tagService;

    @Test
    @DisplayName("관심사 목록 조회에 성공하면 200과 태그 목록을 반환한다")
    void getTags() throws Exception {
        // given
        List<TagResponse> tags = List.of(
                new TagResponse(1L, "장학금"),
                new TagResponse(2L, "취업·채용")
        );
        given(tagService.getTags()).willReturn(tags);

        // when & then
        mockMvc.perform(get("/api/v1/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1L))
                .andExpect(jsonPath("$.data[0].name").value("장학금"))
                .andExpect(jsonPath("$.data[1].name").value("취업·채용"));
    }

    @Test
    @DisplayName("추천 가능한 태그가 없으면 200과 빈 목록을 반환한다")
    void getEmptyTags() throws Exception {
        // given
        given(tagService.getTags()).willReturn(List.of());

        // when & then
        mockMvc.perform(get("/api/v1/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isEmpty());
    }
}
