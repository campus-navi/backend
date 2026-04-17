package com.campusnavi.backend.community.post.service;

import com.campusnavi.backend.community.post.entity.Post;
import com.campusnavi.backend.community.post.entity.PostLike;
import com.campusnavi.backend.community.post.entity.PostScrap;
import com.campusnavi.backend.community.post.repository.PostLikeRepository;
import com.campusnavi.backend.community.post.repository.PostRepository;
import com.campusnavi.backend.community.post.repository.PostScrapRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class PostInteractionServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostLikeRepository postLikeRepository;

    @Mock
    private PostScrapRepository postScrapRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private PostInteractionService postInteractionService;

    private static final Long MEMBER_ID = 1L;
    private static final Long UNIVERSITY_ID = 10L;
    private static final Long POST_ID = 100L;
    private static final AuthMember AUTH_MEMBER = new AuthMember(MEMBER_ID, "USER", UNIVERSITY_ID);

    @Nested
    @DisplayName("좋아요 추가")
    class AddLike {

        @Test
        @DisplayName("좋아요가 없으면 추가하고 likeCount를 증가시킨다")
        void success() {
            // given
            Post post = mock(Post.class);
            Member member = mock(Member.class);
            given(postRepository.findByIdWithMember(POST_ID, UNIVERSITY_ID)).willReturn(Optional.of(post));
            given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
            given(member.getId()).willReturn(MEMBER_ID);
            given(postLikeRepository.findByMemberIdAndPostId(MEMBER_ID, POST_ID)).willReturn(Optional.empty());
            given(postRepository.getReferenceById(POST_ID)).willReturn(post);

            // when
            assertThatCode(() -> postInteractionService.addLike(POST_ID, AUTH_MEMBER))
                    .doesNotThrowAnyException();

            // then
            then(postLikeRepository).should().save(any(PostLike.class));
            then(postRepository).should().incrementLikeCount(POST_ID);
        }

        @Test
        @DisplayName("이미 좋아요가 있으면 아무 작업도 하지 않는다")
        void alreadyLiked() {
            // given
            Post post = mock(Post.class);
            Member member = mock(Member.class);
            PostLike existing = mock(PostLike.class);
            given(postRepository.findByIdWithMember(POST_ID, UNIVERSITY_ID)).willReturn(Optional.of(post));
            given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
            given(member.getId()).willReturn(MEMBER_ID);
            given(postLikeRepository.findByMemberIdAndPostId(MEMBER_ID, POST_ID)).willReturn(Optional.of(existing));

            // when
            assertThatCode(() -> postInteractionService.addLike(POST_ID, AUTH_MEMBER))
                    .doesNotThrowAnyException();

            // then
            then(postLikeRepository).should(never()).save(any());
            then(postRepository).should(never()).incrementLikeCount(anyLong());
        }

        @Test
        @DisplayName("존재하지 않는 게시글이면 POST_NOT_FOUND 예외가 발생한다")
        void postNotFound() {
            // given
            given(postRepository.findByIdWithMember(POST_ID, UNIVERSITY_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> postInteractionService.addLike(POST_ID, AUTH_MEMBER))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.POST_NOT_FOUND));
        }

        @Test
        @DisplayName("존재하지 않는 회원이면 MEMBER_NOT_FOUND 예외가 발생한다")
        void memberNotFound() {
            // given
            Post post = mock(Post.class);
            given(postRepository.findByIdWithMember(POST_ID, UNIVERSITY_ID)).willReturn(Optional.of(post));
            given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> postInteractionService.addLike(POST_ID, AUTH_MEMBER))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.MEMBER_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("좋아요 취소")
    class RemoveLike {

        @Test
        @DisplayName("좋아요가 있으면 삭제하고 likeCount를 감소시킨다")
        void success() {
            // given
            Post post = mock(Post.class);
            PostLike existing = mock(PostLike.class);
            given(postRepository.findByIdWithMember(POST_ID, UNIVERSITY_ID)).willReturn(Optional.of(post));
            given(postLikeRepository.findByMemberIdAndPostId(MEMBER_ID, POST_ID)).willReturn(Optional.of(existing));

            // when
            assertThatCode(() -> postInteractionService.removeLike(POST_ID, AUTH_MEMBER))
                    .doesNotThrowAnyException();

            // then
            then(postLikeRepository).should().delete(existing);
            then(postRepository).should().decrementLikeCount(POST_ID);
        }

        @Test
        @DisplayName("좋아요가 없으면 아무 작업도 하지 않는다")
        void alreadyRemoved() {
            // given
            Post post = mock(Post.class);
            given(postRepository.findByIdWithMember(POST_ID, UNIVERSITY_ID)).willReturn(Optional.of(post));
            given(postLikeRepository.findByMemberIdAndPostId(MEMBER_ID, POST_ID)).willReturn(Optional.empty());

            // when
            assertThatCode(() -> postInteractionService.removeLike(POST_ID, AUTH_MEMBER))
                    .doesNotThrowAnyException();

            // then
            then(postLikeRepository).should(never()).delete(any());
            then(postRepository).should(never()).decrementLikeCount(anyLong());
        }

        @Test
        @DisplayName("존재하지 않는 게시글이면 POST_NOT_FOUND 예외가 발생한다")
        void postNotFound() {
            // given
            given(postRepository.findByIdWithMember(POST_ID, UNIVERSITY_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> postInteractionService.removeLike(POST_ID, AUTH_MEMBER))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.POST_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("스크랩 추가")
    class AddScrap {

        @Test
        @DisplayName("스크랩이 없으면 추가하고 scrapCount를 증가시킨다")
        void success() {
            // given
            Post post = mock(Post.class);
            Member member = mock(Member.class);
            given(postRepository.findByIdWithMember(POST_ID, UNIVERSITY_ID)).willReturn(Optional.of(post));
            given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
            given(member.getId()).willReturn(MEMBER_ID);
            given(postScrapRepository.findByMemberIdAndPostId(MEMBER_ID, POST_ID)).willReturn(Optional.empty());
            given(postRepository.getReferenceById(POST_ID)).willReturn(post);

            // when
            assertThatCode(() -> postInteractionService.addScrap(POST_ID, AUTH_MEMBER))
                    .doesNotThrowAnyException();

            // then
            then(postScrapRepository).should().save(any(PostScrap.class));
            then(postRepository).should().incrementScrapCount(POST_ID);
        }

        @Test
        @DisplayName("이미 스크랩이 있으면 아무 작업도 하지 않는다")
        void alreadyScrapped() {
            // given
            Post post = mock(Post.class);
            Member member = mock(Member.class);
            PostScrap existing = mock(PostScrap.class);
            given(postRepository.findByIdWithMember(POST_ID, UNIVERSITY_ID)).willReturn(Optional.of(post));
            given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
            given(member.getId()).willReturn(MEMBER_ID);
            given(postScrapRepository.findByMemberIdAndPostId(MEMBER_ID, POST_ID)).willReturn(Optional.of(existing));

            // when
            assertThatCode(() -> postInteractionService.addScrap(POST_ID, AUTH_MEMBER))
                    .doesNotThrowAnyException();

            // then
            then(postScrapRepository).should(never()).save(any());
            then(postRepository).should(never()).incrementScrapCount(anyLong());
        }

        @Test
        @DisplayName("존재하지 않는 게시글이면 POST_NOT_FOUND 예외가 발생한다")
        void postNotFound() {
            // given
            given(postRepository.findByIdWithMember(POST_ID, UNIVERSITY_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> postInteractionService.addScrap(POST_ID, AUTH_MEMBER))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.POST_NOT_FOUND));
        }

        @Test
        @DisplayName("존재하지 않는 회원이면 MEMBER_NOT_FOUND 예외가 발생한다")
        void memberNotFound() {
            // given
            Post post = mock(Post.class);
            given(postRepository.findByIdWithMember(POST_ID, UNIVERSITY_ID)).willReturn(Optional.of(post));
            given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> postInteractionService.addScrap(POST_ID, AUTH_MEMBER))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.MEMBER_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("스크랩 취소")
    class RemoveScrap {

        @Test
        @DisplayName("스크랩이 있으면 삭제하고 scrapCount를 감소시킨다")
        void success() {
            // given
            Post post = mock(Post.class);
            PostScrap existing = mock(PostScrap.class);
            given(postRepository.findByIdWithMember(POST_ID, UNIVERSITY_ID)).willReturn(Optional.of(post));
            given(postScrapRepository.findByMemberIdAndPostId(MEMBER_ID, POST_ID)).willReturn(Optional.of(existing));

            // when
            assertThatCode(() -> postInteractionService.removeScrap(POST_ID, AUTH_MEMBER))
                    .doesNotThrowAnyException();

            // then
            then(postScrapRepository).should().delete(existing);
            then(postRepository).should().decrementScrapCount(POST_ID);
        }

        @Test
        @DisplayName("스크랩이 없으면 아무 작업도 하지 않는다")
        void alreadyRemoved() {
            // given
            Post post = mock(Post.class);
            given(postRepository.findByIdWithMember(POST_ID, UNIVERSITY_ID)).willReturn(Optional.of(post));
            given(postScrapRepository.findByMemberIdAndPostId(MEMBER_ID, POST_ID)).willReturn(Optional.empty());

            // when
            assertThatCode(() -> postInteractionService.removeScrap(POST_ID, AUTH_MEMBER))
                    .doesNotThrowAnyException();

            // then
            then(postScrapRepository).should(never()).delete(any());
            then(postRepository).should(never()).decrementScrapCount(anyLong());
        }

        @Test
        @DisplayName("존재하지 않는 게시글이면 POST_NOT_FOUND 예외가 발생한다")
        void postNotFound() {
            // given
            given(postRepository.findByIdWithMember(POST_ID, UNIVERSITY_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> postInteractionService.removeScrap(POST_ID, AUTH_MEMBER))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.POST_NOT_FOUND));
        }
    }
}
