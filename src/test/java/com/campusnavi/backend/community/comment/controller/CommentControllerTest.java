package com.campusnavi.backend.community.comment.controller;

import com.campusnavi.backend.community.comment.dto.CommentCreateRequest;
import com.campusnavi.backend.community.comment.dto.CommentResponse;
import com.campusnavi.backend.community.comment.dto.CommentUpdateRequest;
import com.campusnavi.backend.community.comment.service.CommentInteractionService;
import com.campusnavi.backend.community.comment.service.CommentService;
import com.campusnavi.backend.global.exception.BusinessException;
import com.campusnavi.backend.global.exception.ErrorCode;
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

@ControllerSliceTest(controllers = CommentController.class)
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private CommentService commentService;

    @MockitoBean
    private CommentInteractionService commentInteractionService;

    private static final String BASE_URL = "/api/v1/posts/1/comments";


    @Nested
    @DisplayName("댓글 목록 조회")
    class GetComments {

        @Test
        @DisplayName("유효한 요청이면 200과 댓글 목록을 반환한다")
        void success() throws Exception {
            CommentResponse response = new CommentResponse(1L, "nick", false, "내용", LocalDateTime.now(), 0, 0, false, true, false, List.of());
            given(commentService.getComments(eq(1L), any())).willReturn(List.of(response));

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data[0].content").value("내용"));
        }

        @Test
        @DisplayName("존재하지 않는 게시글이면 404를 반환한다")
        void postNotFound() throws Exception {
            given(commentService.getComments(eq(1L), any()))
                    .willThrow(new BusinessException(ErrorCode.POST_NOT_FOUND));

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.POST_NOT_FOUND.name()));
        }
    }

    @Nested
    @DisplayName("댓글 생성")
    class CreateComment {

        @Test
        @DisplayName("유효한 요청이면 200을 반환한다")
        void success() throws Exception {
            CommentCreateRequest request = new CommentCreateRequest("내용", false);
            willDoNothing().given(commentService).createComment(eq(1L), any(), any());

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("content가 없으면 400을 반환한다")
        void blankContent() throws Exception {
            CommentCreateRequest request = new CommentCreateRequest("", false);

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.name()));
        }
    }

    @Nested
    @DisplayName("답글 생성")
    class CreateReply {

        @Test
        @DisplayName("유효한 요청이면 200을 반환한다")
        void success() throws Exception {
            CommentCreateRequest request = new CommentCreateRequest("답글 내용", false);
            willDoNothing().given(commentService).createReply(eq(1L), eq(1L), any(), any());

            mockMvc.perform(post(BASE_URL + "/1/replies")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("content가 없으면 400을 반환한다")
        void blankContent() throws Exception {
            CommentCreateRequest request = new CommentCreateRequest("", false);

            mockMvc.perform(post(BASE_URL + "/1/replies")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.name()));
        }

        @Test
        @DisplayName("답글 깊이 초과이면 400을 반환한다")
        void replyDepthExceeded() throws Exception {
            CommentCreateRequest request = new CommentCreateRequest("내용", false);
            willThrow(new BusinessException(ErrorCode.REPLY_DEPTH_EXCEEDED))
                    .given(commentService).createReply(eq(1L), eq(1L), any(), any());

            mockMvc.perform(post(BASE_URL + "/1/replies")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.REPLY_DEPTH_EXCEEDED.name()));
        }
    }

    @Nested
    @DisplayName("댓글 수정")
    class UpdateComment {

        @Test
        @DisplayName("유효한 요청이면 200을 반환한다")
        void success() throws Exception {
            CommentUpdateRequest request = new CommentUpdateRequest("수정 내용", false);
            willDoNothing().given(commentService).updateComment(eq(1L), eq(1L), any(), any());

            mockMvc.perform(patch(BASE_URL + "/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("content가 없으면 400을 반환한다")
        void blankContent() throws Exception {
            CommentUpdateRequest request = new CommentUpdateRequest("", false);

            mockMvc.perform(patch(BASE_URL + "/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.name()));
        }

        @Test
        @DisplayName("작성자가 아니면 403을 반환한다")
        void forbidden() throws Exception {
            CommentUpdateRequest request = new CommentUpdateRequest("수정 내용", false);
            willThrow(new BusinessException(ErrorCode.FORBIDDEN))
                    .given(commentService).updateComment(eq(1L), eq(1L), any(), any());

            mockMvc.perform(patch(BASE_URL + "/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.FORBIDDEN.name()));
        }
    }

    @Nested
    @DisplayName("댓글 삭제")
    class DeleteComment {

        @Test
        @DisplayName("유효한 요청이면 200을 반환한다")
        void success() throws Exception {
            willDoNothing().given(commentService).deleteComment(eq(1L), eq(1L), any());

            mockMvc.perform(delete(BASE_URL + "/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("존재하지 않는 댓글이면 404를 반환한다")
        void commentNotFound() throws Exception {
            willThrow(new BusinessException(ErrorCode.COMMENT_NOT_FOUND))
                    .given(commentService).deleteComment(eq(1L), eq(1L), any());

            mockMvc.perform(delete(BASE_URL + "/1"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.COMMENT_NOT_FOUND.name()));
        }

        @Test
        @DisplayName("작성자가 아니면 403을 반환한다")
        void forbidden() throws Exception {
            willThrow(new BusinessException(ErrorCode.FORBIDDEN))
                    .given(commentService).deleteComment(eq(1L), eq(1L), any());

            mockMvc.perform(delete(BASE_URL + "/1"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.FORBIDDEN.name()));
        }
    }

    @Nested
    @DisplayName("댓글 좋아요 추가")
    class AddLike {

        @Test
        @DisplayName("유효한 요청이면 200을 반환한다")
        void success() throws Exception {
            willDoNothing().given(commentInteractionService).addLike(eq(1L), eq(1L), any());

            mockMvc.perform(put(BASE_URL + "/1/likes"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("댓글 좋아요 제거")
    class RemoveLike {

        @Test
        @DisplayName("유효한 요청이면 200을 반환한다")
        void success() throws Exception {
            willDoNothing().given(commentInteractionService).removeLike(eq(1L), eq(1L), any());

            mockMvc.perform(delete(BASE_URL + "/1/likes"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }
}
