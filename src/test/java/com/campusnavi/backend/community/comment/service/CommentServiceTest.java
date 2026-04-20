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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private CommentLikeRepository commentLikeRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private CommentService commentService;

    private static final Long MEMBER_ID = 1L;
    private static final Long UNIVERSITY_ID = 10L;
    private static final Long POST_ID = 100L;
    private static final Long COMMENT_ID = 200L;
    private static final AuthMember AUTH_MEMBER = new AuthMember(MEMBER_ID, "USER", UNIVERSITY_ID);

    @Nested
    @DisplayName("댓글 목록 조회")
    class GetComments {

        @Test
        @DisplayName("정상 요청이면 댓글과 답글 구조를 반환한다")
        void success() {
            // given
            Post post = mockPost(MEMBER_ID);
            Comment parent = mockComment(1L, MEMBER_ID, "내용", false, null, null);
            Comment reply = mockComment(2L, MEMBER_ID, "답글", false, null, parent);

            given(postRepository.findByIdWithMember(POST_ID, UNIVERSITY_ID)).willReturn(Optional.of(post));
            given(commentRepository.findParentComments(POST_ID)).willReturn(List.of(parent));
            given(commentRepository.findRepliesByParentId(List.of(1L))).willReturn(List.of(reply));
            given(reply.getParent()).willReturn(parent);
            given(commentLikeRepository.findLikedCommentIds(MEMBER_ID, List.of(1L, 2L))).willReturn(List.of());

            // when
            List<CommentResponse> result = commentService.getComments(POST_ID, AUTH_MEMBER);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().replies()).hasSize(1);
        }

        @Test
        @DisplayName("익명 댓글이면 nickname이 '익명'으로 반환된다")
        void anonymous() {
            // given
            Post post = mockPost(999L);
            Comment comment = mockComment(1L, 999L, "내용", true, null, null);

            given(postRepository.findByIdWithMember(POST_ID, UNIVERSITY_ID)).willReturn(Optional.of(post));
            given(commentRepository.findParentComments(POST_ID)).willReturn(List.of(comment));
            given(commentRepository.findRepliesByParentId(List.of(1L))).willReturn(List.of());
            given(commentLikeRepository.findLikedCommentIds(MEMBER_ID, List.of(1L))).willReturn(List.of());

            // when
            List<CommentResponse> result = commentService.getComments(POST_ID, AUTH_MEMBER);

            // then
            assertThat(result.getFirst().nickname()).isEqualTo("익명");
        }

        @Test
        @DisplayName("내 댓글이면 isMine이 true이다")
        void isMine() {
            // given
            Post post = mockPost(999L);
            Comment comment = mockComment(1L, MEMBER_ID, "내용", false, null, null);

            given(postRepository.findByIdWithMember(POST_ID, UNIVERSITY_ID)).willReturn(Optional.of(post));
            given(commentRepository.findParentComments(POST_ID)).willReturn(List.of(comment));
            given(commentRepository.findRepliesByParentId(List.of(1L))).willReturn(List.of());
            given(commentLikeRepository.findLikedCommentIds(MEMBER_ID, List.of(1L))).willReturn(List.of());

            // when
            List<CommentResponse> result = commentService.getComments(POST_ID, AUTH_MEMBER);

            // then
            assertThat(result.getFirst().isMine()).isTrue();
        }

        @Test
        @DisplayName("삭제된 댓글은 content가 '삭제된 댓글입니다.'로 반환된다")
        void deletedContent() {
            // given
            Post post = mockPost(999L);
            Comment comment = mockComment(1L, 999L, "원본 내용", false, LocalDateTime.now(), null);

            given(postRepository.findByIdWithMember(POST_ID, UNIVERSITY_ID)).willReturn(Optional.of(post));
            given(commentRepository.findParentComments(POST_ID)).willReturn(List.of(comment));
            given(commentRepository.findRepliesByParentId(List.of(1L))).willReturn(List.of());
            given(commentLikeRepository.findLikedCommentIds(MEMBER_ID, List.of(1L))).willReturn(List.of());

            // when
            List<CommentResponse> result = commentService.getComments(POST_ID, AUTH_MEMBER);

            // then
            assertThat(result.getFirst().content()).isEqualTo("삭제된 댓글입니다.");
            assertThat(result.getFirst().deleted()).isTrue();
        }

        @Test
        @DisplayName("좋아요한 댓글의 isLiked가 true로 반환된다")
        void isLikedTrue() {
            // given
            Post post = mockPost(999L);
            Comment comment = mockComment(1L, 999L, "내용", false, null, null);

            given(postRepository.findByIdWithMember(POST_ID, UNIVERSITY_ID)).willReturn(Optional.of(post));
            given(commentRepository.findParentComments(POST_ID)).willReturn(List.of(comment));
            given(commentRepository.findRepliesByParentId(List.of(1L))).willReturn(List.of());
            given(commentLikeRepository.findLikedCommentIds(MEMBER_ID, List.of(1L))).willReturn(List.of(1L));

            // when
            List<CommentResponse> result = commentService.getComments(POST_ID, AUTH_MEMBER);

            // then
            assertThat(result.getFirst().isLiked()).isTrue();
        }

        @Test
        @DisplayName("좋아요하지 않은 댓글의 isLiked가 false로 반환된다")
        void isLikedFalse() {
            // given
            Post post = mockPost(999L);
            Comment comment = mockComment(1L, 999L, "내용", false, null, null);

            given(postRepository.findByIdWithMember(POST_ID, UNIVERSITY_ID)).willReturn(Optional.of(post));
            given(commentRepository.findParentComments(POST_ID)).willReturn(List.of(comment));
            given(commentRepository.findRepliesByParentId(List.of(1L))).willReturn(List.of());
            given(commentLikeRepository.findLikedCommentIds(MEMBER_ID, List.of(1L))).willReturn(List.of());

            // when
            List<CommentResponse> result = commentService.getComments(POST_ID, AUTH_MEMBER);

            // then
            assertThat(result.getFirst().isLiked()).isFalse();
        }

        @Test
        @DisplayName("댓글이 없으면 빈 리스트를 반환한다")
        void empty() {
            // given
            Post post = mockPost(999L);
            given(postRepository.findByIdWithMember(POST_ID, UNIVERSITY_ID)).willReturn(Optional.of(post));
            given(commentRepository.findParentComments(POST_ID)).willReturn(List.of());

            // when
            List<CommentResponse> result = commentService.getComments(POST_ID, AUTH_MEMBER);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("존재하지 않는 게시글이면 POST_NOT_FOUND 예외가 발생한다")
        void postNotFound() {
            // given
            given(postRepository.findByIdWithMember(POST_ID, UNIVERSITY_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> commentService.getComments(POST_ID, AUTH_MEMBER))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.POST_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("댓글 생성")
    class CreateComment {

        @Test
        @DisplayName("정상 요청이면 댓글을 저장하고 게시글 commentCount를 증가시킨다")
        void success() {
            // given
            CommentCreateRequest request = new CommentCreateRequest("내용", false);
            Post post = mockPost(999L);
            Member member = mock(Member.class);

            given(postRepository.findByIdWithMember(POST_ID, UNIVERSITY_ID)).willReturn(Optional.of(post));
            given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));


            // when
            assertThatCode(() -> commentService.createComment(POST_ID, request, AUTH_MEMBER))
                    .doesNotThrowAnyException();

            // then
            then(commentRepository).should().save(any(Comment.class));
            then(postRepository).should().incrementCommentCount(POST_ID);
        }

        @Test
        @DisplayName("존재하지 않는 게시글이면 POST_NOT_FOUND 예외가 발생한다")
        void postNotFound() {
            // given
            CommentCreateRequest request = new CommentCreateRequest("내용", false);
            given(postRepository.findByIdWithMember(POST_ID, UNIVERSITY_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> commentService.createComment(POST_ID, request, AUTH_MEMBER))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.POST_NOT_FOUND));
        }

        @Test
        @DisplayName("존재하지 않는 회원이면 MEMBER_NOT_FOUND 예외가 발생한다")
        void memberNotFound() {
            // given
            CommentCreateRequest request = new CommentCreateRequest("내용", false);
            Post post = mockPost(999L);

            given(postRepository.findByIdWithMember(POST_ID, UNIVERSITY_ID)).willReturn(Optional.of(post));
            given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> commentService.createComment(POST_ID, request, AUTH_MEMBER))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.MEMBER_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("답글 생성")
    class CreateReply {

        @Test
        @DisplayName("정상 요청이면 답글을 저장하고 게시글/댓글 카운트를 모두 증가시킨다")
        void success() {
            // given
            CommentCreateRequest request = new CommentCreateRequest("답글 내용", false);
            Post post = mockPost(999L);
            Member member = mock(Member.class);
            Comment parent = mockComment(COMMENT_ID, 999L, "부모", false, null, null);
            given(parent.getParent()).willReturn(null);

            given(postRepository.findByIdWithMember(POST_ID, UNIVERSITY_ID)).willReturn(Optional.of(post));
            given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
            given(commentRepository.findByIdAndDeletedAtIsNull(COMMENT_ID)).willReturn(Optional.of(parent));

            // when
            assertThatCode(() -> commentService.createReply(POST_ID, COMMENT_ID, request, AUTH_MEMBER))
                    .doesNotThrowAnyException();

            // then
            then(commentRepository).should().save(any(Comment.class));
            then(postRepository).should().incrementCommentCount(POST_ID);
            then(commentRepository).should().incrementReplyCount(COMMENT_ID);
        }

        @Test
        @DisplayName("존재하지 않는 게시글이면 POST_NOT_FOUND 예외가 발생한다")
        void postNotFound() {
            // given
            CommentCreateRequest request = new CommentCreateRequest("내용", false);
            given(postRepository.findByIdWithMember(POST_ID, UNIVERSITY_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> commentService.createReply(POST_ID, COMMENT_ID, request, AUTH_MEMBER))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.POST_NOT_FOUND));
        }

        @Test
        @DisplayName("부모 댓글이 없으면 COMMENT_NOT_FOUND 예외가 발생한다")
        void commentNotFound() {
            // given
            CommentCreateRequest request = new CommentCreateRequest("내용", false);
            Post post = mockPost(999L);
            Member member = mock(Member.class);

            given(postRepository.findByIdWithMember(POST_ID, UNIVERSITY_ID)).willReturn(Optional.of(post));
            given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
            given(commentRepository.findByIdAndDeletedAtIsNull(COMMENT_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> commentService.createReply(POST_ID, COMMENT_ID, request, AUTH_MEMBER))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.COMMENT_NOT_FOUND));
        }

        @Test
        @DisplayName("답글에 답글을 달면 REPLY_DEPTH_EXCEEDED 예외가 발생한다")
        void replyDepthExceeded() {
            // given
            CommentCreateRequest request = new CommentCreateRequest("내용", false);
            Post post = mockPost(999L);
            Member member = mock(Member.class);
            Comment grandParent = mock(Comment.class);
            Comment parent = mockComment(COMMENT_ID, 999L, "답글", false, null, grandParent);
            given(parent.getParent()).willReturn(grandParent);

            given(postRepository.findByIdWithMember(POST_ID, UNIVERSITY_ID)).willReturn(Optional.of(post));
            given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
            given(commentRepository.findByIdAndDeletedAtIsNull(COMMENT_ID)).willReturn(Optional.of(parent));

            // when & then
            assertThatThrownBy(() -> commentService.createReply(POST_ID, COMMENT_ID, request, AUTH_MEMBER))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.REPLY_DEPTH_EXCEEDED));
        }
    }

    @Nested
    @DisplayName("댓글 수정")
    class UpdateComment {

        @Test
        @DisplayName("작성자가 수정 요청하면 정상 수정된다")
        void success() {
            // given
            CommentUpdateRequest request = new CommentUpdateRequest("수정 내용", true);
            Comment comment = mockComment(COMMENT_ID, MEMBER_ID, "원본", false, null, null);

            given(commentRepository.findByIdAndPostIdAndDeletedAtIsNull(COMMENT_ID, POST_ID))
                    .willReturn(Optional.of(comment));

            // when & then
            assertThatCode(() -> commentService.updateComment(POST_ID, COMMENT_ID, request, AUTH_MEMBER))
                    .doesNotThrowAnyException();
            then(comment).should().update("수정 내용", true);
        }

        @Test
        @DisplayName("존재하지 않는 댓글이면 COMMENT_NOT_FOUND 예외가 발생한다")
        void commentNotFound() {
            // given
            CommentUpdateRequest request = new CommentUpdateRequest("수정 내용", false);
            given(commentRepository.findByIdAndPostIdAndDeletedAtIsNull(COMMENT_ID, POST_ID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> commentService.updateComment(POST_ID, COMMENT_ID, request, AUTH_MEMBER))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.COMMENT_NOT_FOUND));
        }

        @Test
        @DisplayName("작성자가 아니면 FORBIDDEN 예외가 발생한다")
        void forbidden() {
            // given
            CommentUpdateRequest request = new CommentUpdateRequest("수정 내용", false);
            Comment comment = mockComment(COMMENT_ID, 999L, "원본", false, null, null);

            given(commentRepository.findByIdAndPostIdAndDeletedAtIsNull(COMMENT_ID, POST_ID))
                    .willReturn(Optional.of(comment));

            // when & then
            assertThatThrownBy(() -> commentService.updateComment(POST_ID, COMMENT_ID, request, AUTH_MEMBER))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        }
    }

    @Nested
    @DisplayName("댓글 삭제")
    class DeleteComment {

        @Test
        @DisplayName("루트 댓글 삭제 시 softDelete되고 게시글 commentCount가 감소한다")
        void success_rootComment() {
            // given
            Comment comment = mockComment(COMMENT_ID, MEMBER_ID, "내용", false, null, null);
            given(comment.getParent()).willReturn(null);

            given(commentRepository.findByIdAndPostIdAndDeletedAtIsNull(COMMENT_ID, POST_ID))
                    .willReturn(Optional.of(comment));

            // when
            assertThatCode(() -> commentService.deleteComment(POST_ID, COMMENT_ID, AUTH_MEMBER))
                    .doesNotThrowAnyException();

            // then
            then(comment).should().softDelete();
            then(postRepository).should().decrementCommentCount(POST_ID);
            then(commentRepository).shouldHaveNoMoreInteractions();
        }

        @Test
        @DisplayName("답글 삭제 시 부모 댓글 replyCount도 감소한다")
        void success_reply() {
            // given
            Comment parent = mock(Comment.class);
            given(parent.getId()).willReturn(300L);
            Comment reply = mockComment(COMMENT_ID, MEMBER_ID, "답글", false, null, parent);
            given(reply.getParent()).willReturn(parent);

            given(commentRepository.findByIdAndPostIdAndDeletedAtIsNull(COMMENT_ID, POST_ID))
                    .willReturn(Optional.of(reply));

            // when
            commentService.deleteComment(POST_ID, COMMENT_ID, AUTH_MEMBER);

            // then
            then(reply).should().softDelete();
            then(postRepository).should().decrementCommentCount(POST_ID);
            then(commentRepository).should().decrementReplyCount(300L);
        }

        @Test
        @DisplayName("존재하지 않는 댓글이면 COMMENT_NOT_FOUND 예외가 발생한다")
        void commentNotFound() {
            // given
            given(commentRepository.findByIdAndPostIdAndDeletedAtIsNull(COMMENT_ID, POST_ID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> commentService.deleteComment(POST_ID, COMMENT_ID, AUTH_MEMBER))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.COMMENT_NOT_FOUND));
        }

        @Test
        @DisplayName("작성자가 아니면 FORBIDDEN 예외가 발생한다")
        void forbidden() {
            // given
            Comment comment = mockComment(COMMENT_ID, 999L, "내용", false, null, null);

            given(commentRepository.findByIdAndPostIdAndDeletedAtIsNull(COMMENT_ID, POST_ID))
                    .willReturn(Optional.of(comment));

            // when & then
            assertThatThrownBy(() -> commentService.deleteComment(POST_ID, COMMENT_ID, AUTH_MEMBER))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        }
    }

    private Post mockPost(Long authorId) {
        Post post = mock(Post.class);
        Member author = mock(Member.class);
        lenient().when(post.getMember()).thenReturn(author);
        lenient().when(author.getId()).thenReturn(authorId);
        return post;
    }

    private Comment mockComment(Long id, Long memberId, String content, boolean anonymous,
                                LocalDateTime deletedAt, Comment parent) {
        Comment comment = mock(Comment.class);
        Member member = mock(Member.class);
        lenient().when(comment.getId()).thenReturn(id);
        lenient().when(comment.getMember()).thenReturn(member);
        lenient().when(member.getId()).thenReturn(memberId);
        lenient().when(member.getNickname()).thenReturn("nick" + memberId);
        lenient().when(comment.getContent()).thenReturn(content);
        lenient().when(comment.isAnonymous()).thenReturn(anonymous);
        lenient().when(comment.getDeletedAt()).thenReturn(deletedAt);
        lenient().when(comment.getParent()).thenReturn(parent);
        lenient().when(comment.getCreatedAt()).thenReturn(LocalDateTime.now());
        lenient().when(comment.getLikeCount()).thenReturn(0);
        lenient().when(comment.getReplyCount()).thenReturn(0);
        return comment;
    }
}
