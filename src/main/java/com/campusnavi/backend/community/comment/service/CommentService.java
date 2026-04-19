package com.campusnavi.backend.community.comment.service;

import com.campusnavi.backend.community.comment.dto.CommentCreateRequest;
import com.campusnavi.backend.community.comment.dto.CommentResponse;
import com.campusnavi.backend.community.comment.dto.CommentUpdateRequest;
import com.campusnavi.backend.community.comment.entity.Comment;
import com.campusnavi.backend.community.comment.repository.CommentLikeRepository;
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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final PostRepository postRepository;
    private final MemberRepository memberRepository;

    public List<CommentResponse> getComments(Long postId, AuthMember authMember) {
        Post post = postRepository.findByIdWithMember(postId, authMember.universityId())
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        Long postAuthorId = post.getMember().getId();

        List<Comment> rootComments = commentRepository.findParentComments(postId);

        if (rootComments.isEmpty()) {
            return List.of();
        }

        List<Long> rootIds = rootComments.stream().map(Comment::getId).toList();
        List<Comment> replies = commentRepository.findRepliesByParentId(rootIds);

        Map<Long, List<Comment>> replyMap = replies.stream()
                .collect(Collectors.groupingBy(r -> r.getParent().getId()));

        List<Long> allIds = Stream.concat(rootComments.stream(), replies.stream())
                .map(Comment::getId)
                .toList();
        Set<Long> likedIds = new HashSet<>(commentLikeRepository.findLikedCommentIds(authMember.memberId(), allIds));

        return rootComments.stream()
                .map(comment -> toResponse(comment, postAuthorId, authMember, replyMap, likedIds))
                .toList();
    }

    private CommentResponse toResponse(Comment comment, Long postAuthorId, AuthMember authMember,
                                        Map<Long, List<Comment>> replyMap, Set<Long> likedIds) {
        Member member = comment.getMember();
        String nickname = comment.isAnonymous() ? "익명" : member.getNickname();
        boolean isAuthor = member.getId().equals(postAuthorId);
        boolean isMine = member.getId().equals(authMember.memberId());
        boolean isDeleted = comment.getDeletedAt() != null;
        boolean isLiked = likedIds.contains(comment.getId());

        List<CommentResponse> replies = replyMap.getOrDefault(comment.getId(), List.of())
                .stream()
                .map(reply -> toResponse(reply, postAuthorId, authMember, replyMap, likedIds))
                .toList();

        return new CommentResponse(
                comment.getId(),
                nickname,
                isAuthor,
                isDeleted ? "삭제된 댓글입니다." : comment.getContent(),
                comment.getCreatedAt(),
                comment.getLikeCount(),
                comment.getReplyCount(),
                isLiked,
                isMine,
                isDeleted,
                replies
        );
    }

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
