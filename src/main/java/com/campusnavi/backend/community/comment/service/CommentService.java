package com.campusnavi.backend.community.comment.service;

import com.campusnavi.backend.community.comment.dto.CommentCreateRequest;
import com.campusnavi.backend.community.comment.dto.CommentUpdateRequest;
import com.campusnavi.backend.community.comment.entity.Comment;
import com.campusnavi.backend.community.comment.repository.CommentRepository;
import com.campusnavi.backend.community.post.entity.Post;
import com.campusnavi.backend.community.post.repository.PostRepository;
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
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public void createComment(Long postId, CommentCreateRequest request, AuthMember authMember) {
        Post post = postRepository.findByIdWithMember(postId, authMember.universityId())
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        Member member = memberRepository.findById(authMember.memberId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        Comment comment = Comment.create(post, member, null, request.content(), request.isAnonymous());

        commentRepository.save(comment);
        postRepository.incrementCommentCount(postId);
    }

    @Transactional
    public void createReply(Long postId, Long commentId, CommentCreateRequest request, AuthMember authMember) {
        Post post = postRepository.findByIdWithMember(postId, authMember.universityId())
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        Member member = memberRepository.findById(authMember.memberId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        Comment parent = commentRepository.findByIdAndDeletedAtIsNull(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));

        if (parent.getParent() != null) {
            throw new BusinessException(ErrorCode.REPLY_DEPTH_EXCEEDED);
        }

        Comment comment = Comment.create(post, member, parent, request.content(), request.isAnonymous());

        commentRepository.save(comment);
        postRepository.incrementCommentCount(postId);
        commentRepository.incrementCommentCount(commentId);
    }

    @Transactional
    public void updateComment(Long postId, Long commentId, CommentUpdateRequest request, AuthMember authMember) {
        Comment comment = commentRepository.findByIdAndPostIdAndDeletedAtIsNull(commentId, postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));

        if (!comment.getMember().getId().equals(authMember.memberId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        comment.update(request.content(), request.isAnonymous());
    }

    @Transactional
    public void deleteComment(Long postId, Long commentId, AuthMember authMember) {
        Comment comment = commentRepository.findByIdAndPostIdAndDeletedAtIsNull(commentId, postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));

        if (!comment.getMember().getId().equals(authMember.memberId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        comment.softDelete();
        postRepository.decrementCommentCount(postId);
        if (comment.getParent() != null) {
            commentRepository.decrementCommentCount(comment.getParent().getId());
        }
    }
}
