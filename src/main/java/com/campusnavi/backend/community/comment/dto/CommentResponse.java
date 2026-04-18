package com.campusnavi.backend.community.comment.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CommentResponse(
        Long id,
        String nickname,
        boolean isAuthor,
        String content,
        LocalDateTime createdAt,
        int likeCount,
        int replyCount,
        boolean isLiked,
        boolean isMine,
        boolean deleted,
        List<CommentResponse> replies
) {
}
