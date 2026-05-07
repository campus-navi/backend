package com.campusnavi.backend.tag.controller;

import com.campusnavi.backend.support.ControllerSliceTest;
import com.campusnavi.backend.tag.dto.InternalTagResponse;
import com.campusnavi.backend.tag.service.TagService;
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

@ControllerSliceTest(controllers = TagInternalController.class)
class TagInternalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TagService tagService;

    @Test
    @DisplayName("내부 API 태그 목록 조회에 성공하면 200과 code·name 목록을 반환한다")
    void getTagsForInternal() throws Exception {
        // given
        List<InternalTagResponse> tags = List.of(
                new InternalTagResponse("SCHOLARSHIP", "장학금"),
                new InternalTagResponse("EMPLOYMENT", "취업·채용")
        );
        given(tagService.getAllTagsForInternal()).willReturn(tags);

        // when & then
        mockMvc.perform(get("/internal/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].code").value("SCHOLARSHIP"))
                .andExpect(jsonPath("$.data[0].name").value("장학금"))
                .andExpect(jsonPath("$.data[1].code").value("EMPLOYMENT"))
                .andExpect(jsonPath("$.data[1].name").value("취업·채용"));
    }

    @Test
    @DisplayName("등록된 태그가 없으면 200과 빈 목록을 반환한다")
    void getEmptyTagsForInternal() throws Exception {
        // given
        given(tagService.getAllTagsForInternal()).willReturn(List.of());

        // when & then
        mockMvc.perform(get("/internal/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isEmpty());
    }
}
