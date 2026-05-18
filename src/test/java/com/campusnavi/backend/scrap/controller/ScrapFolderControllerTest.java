package com.campusnavi.backend.scrap.controller;

import com.campusnavi.backend.global.exception.BusinessException;
import com.campusnavi.backend.global.exception.ErrorCode;
import com.campusnavi.backend.global.security.AuthMember;
import com.campusnavi.backend.scrap.dto.ScrapFolderCreateRequest;
import com.campusnavi.backend.scrap.dto.ScrapFolderResponse;
import com.campusnavi.backend.scrap.dto.ScrapFolderUpdateRequest;
import com.campusnavi.backend.scrap.service.ScrapFolderService;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ControllerSliceTest(controllers = ScrapFolderController.class)
class ScrapFolderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private ScrapFolderService scrapFolderService;

    private static final Long MEMBER_ID = 1L;
    private static final Authentication AUTH = new UsernamePasswordAuthenticationToken(
            new AuthMember(MEMBER_ID, "USER", 10L), null, List.of()
    );

    @Nested
    @DisplayName("폴더 생성")
    class Create {

        @Test
        @DisplayName("정상 요청이면 201을 반환한다")
        void success() throws Exception {
            ScrapFolderCreateRequest request = new ScrapFolderCreateRequest("취업", "취업 공고");
            willDoNothing().given(scrapFolderService).create(eq(MEMBER_ID), any());

            mockMvc.perform(post("/api/v1/scrap-folders").with(authentication(AUTH))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("이름이 비어 있으면 400을 반환한다")
        void blankName() throws Exception {
            ScrapFolderCreateRequest request = new ScrapFolderCreateRequest("", null);

            mockMvc.perform(post("/api/v1/scrap-folders").with(authentication(AUTH))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.name()));
        }

        @Test
        @DisplayName("이름이 중복되면 409를 반환한다")
        void duplicateName() throws Exception {
            ScrapFolderCreateRequest request = new ScrapFolderCreateRequest("취업", null);
            willThrow(new BusinessException(ErrorCode.SCRAP_FOLDER_NAME_DUPLICATE))
                    .given(scrapFolderService).create(eq(MEMBER_ID), any());

            mockMvc.perform(post("/api/v1/scrap-folders").with(authentication(AUTH))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value(ErrorCode.SCRAP_FOLDER_NAME_DUPLICATE.name()));
        }
    }

    @Nested
    @DisplayName("폴더 수정")
    class Update {

        @Test
        @DisplayName("정상 요청이면 200을 반환한다")
        void success() throws Exception {
            ScrapFolderUpdateRequest request = new ScrapFolderUpdateRequest("장학", "장학 공지");
            willDoNothing().given(scrapFolderService).update(eq(MEMBER_ID), eq(100L), any());

            mockMvc.perform(patch("/api/v1/scrap-folders/100").with(authentication(AUTH))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("존재하지 않거나 타인 폴더면 404를 반환한다")
        void notFound() throws Exception {
            ScrapFolderUpdateRequest request = new ScrapFolderUpdateRequest("장학", null);
            willThrow(new BusinessException(ErrorCode.SCRAP_FOLDER_NOT_FOUND))
                    .given(scrapFolderService).update(eq(MEMBER_ID), eq(100L), any());

            mockMvc.perform(patch("/api/v1/scrap-folders/100").with(authentication(AUTH))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(ErrorCode.SCRAP_FOLDER_NOT_FOUND.name()));
        }
    }

    @Nested
    @DisplayName("폴더 삭제")
    class Delete {

        @Test
        @DisplayName("정상 요청이면 200을 반환한다")
        void success() throws Exception {
            willDoNothing().given(scrapFolderService).delete(MEMBER_ID, 100L);

            mockMvc.perform(delete("/api/v1/scrap-folders/100").with(authentication(AUTH)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("폴더 목록 조회")
    class GetFolders {

        @Test
        @DisplayName("정상 요청이면 200을 반환한다")
        void success() throws Exception {
            given(scrapFolderService.getFolders(eq(MEMBER_ID), any()))
                    .willReturn(List.of(new ScrapFolderResponse(100L, "취업", null, 0L)));

            mockMvc.perform(get("/api/v1/scrap-folders").with(authentication(AUTH)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].name").value("취업"));
        }
    }
}
