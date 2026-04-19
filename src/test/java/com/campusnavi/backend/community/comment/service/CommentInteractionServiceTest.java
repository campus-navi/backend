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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class CommentInteractionServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private CommentLikeRepository commentLikeRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private CommentInteractionService commentInteractionService;

    private static final Long MEMBER_ID = 1L;
    private static final Long UNIVERSITY_ID = 10L;
    private static final Long POST_ID = 100L;
    private static final Long COMMENT_ID = 200L;
    private static final AuthMember AUTH_MEMBER = new AuthMember(MEMBER_ID, "USER", UNIVERSITY_ID);

    @Nested
    @DisplayName("좋아요 추가")
    class AddLike {

        @Test
        @DisplayName("정상 요청이면 CommentLike를 저장하고 likeCount를 증가시킨다")
        void success() {
            Comment comment = mock(Comment.class);
            Member member = mock(Member.class);
            given(member.getId()).willReturn(MEMBER_ID);
            given(commentRepository.findByIdAndPostIdAndDeletedAtIsNull(COMMENT_ID, POST_ID))
                    .willReturn(Optional.of(comment));
            given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
            given(commentLikeRepository.findByMemberIdAndCommentId(MEMBER_ID, COMMENT_ID))
                    .willReturn(Optional.empty());

            assertThatCode(() -> commentInteractionService.addLike(POST_ID, COMMENT_ID, AUTH_MEMBER))
                    .doesNotThrowAnyException();

            then(commentLikeRepository).should().save(any(CommentLike.class));
            then(commentRepository).should().incrementLikeCount(COMMENT_ID);
        }

        @Test
        @DisplayName("이미 좋아요한 경우 저장 및 카운트 증가를 하지 않는다")
        void duplicateLikeIgnored() {
            Comment comment = mock(Comment.class);
            Member member = mock(Member.class);
            given(member.getId()).willReturn(MEMBER_ID);
            given(commentRepository.findByIdAndPostIdAndDeletedAtIsNull(COMMENT_ID, POST_ID))
                    .willReturn(Optional.of(comment));
            given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
            given(commentLikeRepository.findByMemberIdAndCommentId(MEMBER_ID, COMMENT_ID))
                    .willReturn(Optional.of(mock(CommentLike.class)));

            assertThatCode(() -> commentInteractionService.addLike(POST_ID, COMMENT_ID, AUTH_MEMBER))
                    .doesNotThrowAnyException();

            then(commentLikeRepository).shouldHaveNoMoreInteractions();
            then(commentRepository).should().findByIdAndPostIdAndDeletedAtIsNull(COMMENT_ID, POST_ID);
            then(commentRepository).shouldHaveNoMoreInteractions();
        }

        @Test
        @DisplayName("존재하지 않는 댓글이면 COMMENT_NOT_FOUND 예외가 발생한다")
        void commentNotFound() {
            given(commentRepository.findByIdAndPostIdAndDeletedAtIsNull(COMMENT_ID, POST_ID))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> commentInteractionService.addLike(POST_ID, COMMENT_ID, AUTH_MEMBER))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.COMMENT_NOT_FOUND));
        }

        @Test
        @DisplayName("존재하지 않는 회원이면 MEMBER_NOT_FOUND 예외가 발생한다")
        void memberNotFound() {
            Comment comment = mock(Comment.class);
            given(commentRepository.findByIdAndPostIdAndDeletedAtIsNull(COMMENT_ID, POST_ID))
                    .willReturn(Optional.of(comment));
            given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> commentInteractionService.addLike(POST_ID, COMMENT_ID, AUTH_MEMBER))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.MEMBER_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("좋아요 제거")
    class RemoveLike {

        @Test
        @DisplayName("좋아요가 있으면 삭제하고 likeCount를 감소시킨다")
        void success() {
            Comment comment = mock(Comment.class);
            CommentLike like = mock(CommentLike.class);
            given(commentRepository.findByIdAndPostIdAndDeletedAtIsNull(COMMENT_ID, POST_ID))
                    .willReturn(Optional.of(comment));
            given(commentLikeRepository.findByMemberIdAndCommentId(MEMBER_ID, COMMENT_ID))
                    .willReturn(Optional.of(like));

            assertThatCode(() -> commentInteractionService.removeLike(POST_ID, COMMENT_ID, AUTH_MEMBER))
                    .doesNotThrowAnyException();

            then(commentLikeRepository).should().delete(like);
            then(commentRepository).should().decrementLikeCount(COMMENT_ID);
        }

        @Test
        @DisplayName("좋아요가 없으면 아무 것도 하지 않는다")
        void likeNotFound() {
            Comment comment = mock(Comment.class);
            given(commentRepository.findByIdAndPostIdAndDeletedAtIsNull(COMMENT_ID, POST_ID))
                    .willReturn(Optional.of(comment));
            given(commentLikeRepository.findByMemberIdAndCommentId(MEMBER_ID, COMMENT_ID))
                    .willReturn(Optional.empty());

            assertThatCode(() -> commentInteractionService.removeLike(POST_ID, COMMENT_ID, AUTH_MEMBER))
                    .doesNotThrowAnyException();

            then(commentLikeRepository).should().findByMemberIdAndCommentId(MEMBER_ID, COMMENT_ID);
            then(commentLikeRepository).shouldHaveNoMoreInteractions();
            then(commentRepository).should().findByIdAndPostIdAndDeletedAtIsNull(COMMENT_ID, POST_ID);
            then(commentRepository).shouldHaveNoMoreInteractions();
        }

        @Test
        @DisplayName("존재하지 않는 댓글이면 COMMENT_NOT_FOUND 예외가 발생한다")
        void commentNotFound() {
            given(commentRepository.findByIdAndPostIdAndDeletedAtIsNull(COMMENT_ID, POST_ID))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> commentInteractionService.removeLike(POST_ID, COMMENT_ID, AUTH_MEMBER))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.COMMENT_NOT_FOUND));
        }
    }
}
