package com.campusnavi.backend.community.comment.service;

import com.campusnavi.backend.community.comment.entity.Comment;
import com.campusnavi.backend.community.comment.entity.CommentLike;
import com.campusnavi.backend.community.comment.repository.CommentLikeRepository;
import com.campusnavi.backend.community.comment.repository.CommentRepository;
import com.campusnavi.backend.global.exception.BusinessException;
import com.campusnavi.backend.global.exception.ErrorCode;
import com.campusnavi.backend.global.security.AuthMember;
import com.campusnavi.backend.member.entity.Member;
import com.campusnavi.backend.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentInteractionService {

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final MemberRepository memberRepository;

    public void addLike(Long postId, Long commentId, AuthMember authMember) {
        Comment comment = commentRepository.findByIdAndPostIdAndDeletedAtIsNull(commentId, postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));

        Member member = memberRepository.findById(authMember.memberId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        if (commentLikeRepository.findByMemberIdAndCommentId(member.getId(), commentId).isPresent()) {
            return;
        }

        CommentLike like = CommentLike.create(member, comment);
        commentLikeRepository.save(like);
        commentRepository.incrementLikeCount(commentId);
    }

    public void removeLike(Long postId, Long commentId, AuthMember authMember) {
        Comment comment = commentRepository.findByIdAndPostIdAndDeletedAtIsNull(commentId, postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));

        commentLikeRepository.findByMemberIdAndCommentId(authMember.memberId(), commentId)
                .ifPresent(like -> {
                    commentLikeRepository.delete(like);
                    commentRepository.decrementLikeCount(commentId);
                });
    }
}
