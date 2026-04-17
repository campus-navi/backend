package com.campusnavi.backend.community.post.controller;

import com.campusnavi.backend.community.post.dto.PostCreateRequest;
import com.campusnavi.backend.community.post.dto.PostCreateResponse;
import com.campusnavi.backend.community.post.dto.PostPresignedUrlRequest;
import com.campusnavi.backend.community.post.dto.PostResponse;
import com.campusnavi.backend.community.post.dto.PostUpdateRequest;
import com.campusnavi.backend.community.post.service.PostService;
import com.campusnavi.backend.global.exception.BusinessException;
import com.campusnavi.backend.global.exception.ErrorCode;
import com.campusnavi.backend.infra.storage.PresignedUrlResponse;
import com.campusnavi.backend.support.ControllerSliceTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ControllerSliceTest(controllers = PostController.class)
class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private PostService postService;

    @Nested
    @DisplayName("게시글 생성")
    class CreatePost {

        @Test
        @DisplayName("유효한 요청이면 200과 postId를 반환한다")
        void success() throws Exception {
            PostCreateRequest request = new PostCreateRequest("제목", "내용", false, List.of());
            given(postService.createPost(any(), any())).willReturn(new PostCreateResponse(1L));

            mockMvc.perform(post("/api/v1/posts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(1L));
        }

        @Test
        @DisplayName("title이 없으면 400을 반환한다")
        void blankTitle() throws Exception {
            PostCreateRequest request = new PostCreateRequest("", "내용", false, null);

            mockMvc.perform(post("/api/v1/posts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.name()));
        }
    }

    @Nested
    @DisplayName("게시글 조회")
    class GetPost {

        @Test
        @DisplayName("유효한 요청이면 200과 게시글 정보를 반환한다")
        void success() throws Exception {
            PostResponse response = new PostResponse("nick", "제목", "내용",
                    LocalDateTime.now(), 0, 0, 0, List.of(), false, false, true);
            given(postService.getPost(eq(1L), any())).willReturn(response);

            mockMvc.perform(get("/api/v1/posts/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.title").value("제목"))
                    .andExpect(jsonPath("$.data.isMine").value(true));
        }

        @Test
        @DisplayName("존재하지 않는 게시글이면 404를 반환한다")
        void postNotFound() throws Exception {
            given(postService.getPost(eq(999L), any()))
                    .willThrow(new BusinessException(ErrorCode.POST_NOT_FOUND));

            mockMvc.perform(get("/api/v1/posts/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.POST_NOT_FOUND.name()));
        }
    }

    @Nested
    @DisplayName("Presigned URL 발급")
    class GeneratePresignedUrl {

        @Test
        @DisplayName("유효한 요청이면 200과 presignedUrl을 반환한다")
        void success() throws Exception {
            PostPresignedUrlRequest request = new PostPresignedUrlRequest("photo.jpg", "image/jpeg", 1024L);
            given(postService.generatePostPresignedUrl(any()))
                    .willReturn(new PresignedUrlResponse("https://s3.example.com/upload", "post-images/uuid.jpg"));

            mockMvc.perform(post("/api/v1/posts/presigned-url")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.url").value("https://s3.example.com/upload"));
        }

        @Test
        @DisplayName("filename이 없으면 400을 반환한다")
        void blankFilename() throws Exception {
            PostPresignedUrlRequest request = new PostPresignedUrlRequest("", "image/jpeg", 1024L);

            mockMvc.perform(post("/api/v1/posts/presigned-url")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.name()));
        }

        @Test
        @DisplayName("size가 0 이하이면 400을 반환한다")
        void invalidSize() throws Exception {
            PostPresignedUrlRequest request = new PostPresignedUrlRequest("photo.jpg", "image/jpeg", 0L);

            mockMvc.perform(post("/api/v1/posts/presigned-url")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.name()));
        }

        @Test
        @DisplayName("이미지가 아닌 contentType이면 400을 반환한다")
        void invalidContentType() throws Exception {
            PostPresignedUrlRequest request = new PostPresignedUrlRequest("file.pdf", "application/pdf", 1024L);
            given(postService.generatePostPresignedUrl(any()))
                    .willThrow(new BusinessException(ErrorCode.INVALID_CONTENT_TYPE));

            mockMvc.perform(post("/api/v1/posts/presigned-url")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_CONTENT_TYPE.name()));
        }
    }

    @Nested
    @DisplayName("게시글 수정")
    class UpdatePost {

        @Test
        @DisplayName("유효한 요청이면 200을 반환한다")
        void success() throws Exception {
            PostUpdateRequest request = new PostUpdateRequest("새 제목", "새 내용", false, List.of());
            willDoNothing().given(postService).updatePost(eq(1L), any(), any());

            mockMvc.perform(patch("/api/v1/posts/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("title이 없으면 400을 반환한다")
        void blankTitle() throws Exception {
            PostUpdateRequest request = new PostUpdateRequest("", "새 내용", false, null);

            mockMvc.perform(patch("/api/v1/posts/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.name()));
        }

        @Test
        @DisplayName("작성자가 아니면 403을 반환한다")
        void forbidden() throws Exception {
            PostUpdateRequest request = new PostUpdateRequest("제목", "내용", false, null);
            willThrow(new BusinessException(ErrorCode.FORBIDDEN))
                    .given(postService).updatePost(eq(1L), any(), any());

            mockMvc.perform(patch("/api/v1/posts/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.FORBIDDEN.name()));
        }
    }

    @Nested
    @DisplayName("게시글 삭제")
    class DeletePost {

        @Test
        @DisplayName("유효한 요청이면 200을 반환한다")
        void success() throws Exception {
            willDoNothing().given(postService).deletePost(eq(1L), any());

            mockMvc.perform(delete("/api/v1/posts/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("존재하지 않는 게시글이면 404를 반환한다")
        void postNotFound() throws Exception {
            willThrow(new BusinessException(ErrorCode.POST_NOT_FOUND))
                    .given(postService).deletePost(eq(999L), any());

            mockMvc.perform(delete("/api/v1/posts/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.POST_NOT_FOUND.name()));
        }

        @Test
        @DisplayName("작성자가 아니면 403을 반환한다")
        void forbidden() throws Exception {
            willThrow(new BusinessException(ErrorCode.FORBIDDEN))
                    .given(postService).deletePost(eq(1L), any());

            mockMvc.perform(delete("/api/v1/posts/1"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.FORBIDDEN.name()));
        }
    }
}
