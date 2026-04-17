package com.campusnavi.backend.community.post.service;

import com.campusnavi.backend.community.post.dto.PostCreateRequest;
import com.campusnavi.backend.community.post.dto.PostCreateResponse;
import com.campusnavi.backend.community.post.dto.PostPresignedUrlRequest;
import com.campusnavi.backend.community.post.dto.PostResponse;
import com.campusnavi.backend.community.post.dto.PostUpdateRequest;
import com.campusnavi.backend.community.post.entity.Post;
import com.campusnavi.backend.community.post.entity.PostImage;
import com.campusnavi.backend.community.post.repository.PostImageRepository;
import com.campusnavi.backend.community.post.repository.PostRepository;
import com.campusnavi.backend.global.exception.BusinessException;
import com.campusnavi.backend.global.exception.ErrorCode;
import com.campusnavi.backend.global.security.AuthMember;
import com.campusnavi.backend.infra.storage.PresignedUrlResponse;
import com.campusnavi.backend.infra.storage.S3StorageService;
import com.campusnavi.backend.infra.storage.UploadType;
import com.campusnavi.backend.member.entity.Member;
import com.campusnavi.backend.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostImageRepository imageRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private S3StorageService s3StorageService;

    @InjectMocks
    private PostService postService;

    private static final Long MEMBER_ID = 1L;
    private static final Long UNIVERSITY_ID = 10L;
    private static final Long POST_ID = 100L;
    private static final AuthMember AUTH_MEMBER = new AuthMember(MEMBER_ID, "USER", UNIVERSITY_ID);

    @Nested
    @DisplayName("게시글 생성")
    class CreatePost {

        @Test
        @DisplayName("정상 요청이면 게시글을 저장하고 PostCreateResponse를 반환한다")
        void success() {
            // given
            PostCreateRequest request = new PostCreateRequest("제목", "내용", false, List.of("key1", "key2"));
            Member member = mock(Member.class);
            given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
            given(member.getUniversityId()).willReturn(UNIVERSITY_ID);
            given(member.getNickname()).willReturn("nick");

            // when
            PostCreateResponse response = postService.createPost(AUTH_MEMBER, request);

            // then
            then(postRepository).should().save(any(Post.class));
            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("존재하지 않는 회원이면 MEMBER_NOT_FOUND 예외가 발생한다")
        void memberNotFound() {
            // given
            PostCreateRequest request = new PostCreateRequest("제목", "내용", false, null);
            given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> postService.createPost(AUTH_MEMBER, request))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.MEMBER_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("게시글 조회")
    class GetPost {

        @Test
        @DisplayName("내 게시글을 조회하면 isMine이 true인 PostResponse를 반환한다")
        void success_myPost() {
            // given
            Post post = mock(Post.class);
            Member member = mock(Member.class);
            given(postRepository.findByIdWithMember(POST_ID, UNIVERSITY_ID)).willReturn(Optional.of(post));
            given(post.getMember()).willReturn(member);
            given(member.getId()).willReturn(MEMBER_ID);
            given(member.getNickname()).willReturn("nick");
            given(post.isAnonymous()).willReturn(false);
            given(imageRepository.findByPostIdOrderBySortOrderAsc(POST_ID)).willReturn(List.of());

            // when
            PostResponse response = postService.getPost(POST_ID, AUTH_MEMBER);

            // then
            assertThat(response.isMine()).isTrue();
        }

        @Test
        @DisplayName("익명 게시글이면 닉네임이 '익명'으로 반환된다")
        void anonymousPost() {
            // given
            Post post = mock(Post.class);
            Member member = mock(Member.class);
            given(postRepository.findByIdWithMember(POST_ID, UNIVERSITY_ID)).willReturn(Optional.of(post));
            given(post.getMember()).willReturn(member);
            given(member.getId()).willReturn(999L);
            given(member.getNickname()).willReturn("nick");
            given(post.isAnonymous()).willReturn(true);
            given(imageRepository.findByPostIdOrderBySortOrderAsc(POST_ID)).willReturn(List.of());

            // when
            PostResponse response = postService.getPost(POST_ID, AUTH_MEMBER);

            // then
            assertThat(response.nickname()).isEqualTo("익명");
            assertThat(response.isMine()).isFalse();
        }

        @Test
        @DisplayName("존재하지 않는 게시글이면 POST_NOT_FOUND 예외가 발생한다")
        void postNotFound() {
            // given
            given(postRepository.findByIdWithMember(POST_ID, UNIVERSITY_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> postService.getPost(POST_ID, AUTH_MEMBER))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.POST_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("게시글 수정")
    class UpdatePost {

        @Test
        @DisplayName("작성자가 수정 요청하면 게시글이 정상 수정된다")
        void success() {
            // given
            PostUpdateRequest request = new PostUpdateRequest("새 제목", "새 내용", true, List.of());
            Post post = mock(Post.class);
            Member member = mock(Member.class);
            given(postRepository.findByIdWithMember(POST_ID, UNIVERSITY_ID)).willReturn(Optional.of(post));
            given(post.getMember()).willReturn(member);
            given(member.getId()).willReturn(MEMBER_ID);
            given(imageRepository.findByPostId(POST_ID)).willReturn(List.of());

            // when & then
            assertThatCode(() -> postService.updatePost(POST_ID, AUTH_MEMBER, request))
                    .doesNotThrowAnyException();
            then(post).should().update("새 제목", "새 내용", true);
        }

        @Test
        @DisplayName("존재하지 않는 게시글이면 POST_NOT_FOUND 예외가 발생한다")
        void postNotFound() {
            // given
            PostUpdateRequest request = new PostUpdateRequest("제목", "내용", false, null);
            given(postRepository.findByIdWithMember(POST_ID, UNIVERSITY_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> postService.updatePost(POST_ID, AUTH_MEMBER, request))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.POST_NOT_FOUND));
        }

        @Test
        @DisplayName("작성자가 아니면 FORBIDDEN 예외가 발생한다")
        void forbidden() {
            // given
            PostUpdateRequest request = new PostUpdateRequest("제목", "내용", false, null);
            Post post = mock(Post.class);
            Member member = mock(Member.class);
            given(postRepository.findByIdWithMember(POST_ID, UNIVERSITY_ID)).willReturn(Optional.of(post));
            given(post.getMember()).willReturn(member);
            given(member.getId()).willReturn(999L);

            // when & then
            assertThatThrownBy(() -> postService.updatePost(POST_ID, AUTH_MEMBER, request))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        }
    }

    @Nested
    @DisplayName("게시글 삭제")
    class DeletePost {

        @Test
        @DisplayName("작성자가 삭제 요청하면 게시글이 softDelete된다")
        void success() {
            // given
            Post post = mock(Post.class);
            Member member = mock(Member.class);
            given(postRepository.findByIdWithMember(POST_ID, UNIVERSITY_ID)).willReturn(Optional.of(post));
            given(post.getMember()).willReturn(member);
            given(member.getId()).willReturn(MEMBER_ID);
            given(imageRepository.findByPostId(POST_ID)).willReturn(List.of());

            // when & then
            assertThatCode(() -> postService.deletePost(POST_ID, AUTH_MEMBER))
                    .doesNotThrowAnyException();
            then(post).should().softDelete();
        }

        @Test
        @DisplayName("존재하지 않는 게시글이면 POST_NOT_FOUND 예외가 발생한다")
        void postNotFound() {
            // given
            given(postRepository.findByIdWithMember(POST_ID, UNIVERSITY_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> postService.deletePost(POST_ID, AUTH_MEMBER))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.POST_NOT_FOUND));
        }

        @Test
        @DisplayName("작성자가 아니면 FORBIDDEN 예외가 발생한다")
        void forbidden() {
            // given
            Post post = mock(Post.class);
            Member member = mock(Member.class);
            given(postRepository.findByIdWithMember(POST_ID, UNIVERSITY_ID)).willReturn(Optional.of(post));
            given(post.getMember()).willReturn(member);
            given(member.getId()).willReturn(999L);

            // when & then
            assertThatThrownBy(() -> postService.deletePost(POST_ID, AUTH_MEMBER))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        }

        @Test
        @DisplayName("이미지가 있으면 S3에서 삭제 후 DB에서도 삭제한다")
        void deleteWithImages() {
            // given
            Post post = mock(Post.class);
            Member member = mock(Member.class);
            PostImage image = mock(PostImage.class);
            given(postRepository.findByIdWithMember(POST_ID, UNIVERSITY_ID)).willReturn(Optional.of(post));
            given(post.getMember()).willReturn(member);
            given(member.getId()).willReturn(MEMBER_ID);
            given(imageRepository.findByPostId(POST_ID)).willReturn(List.of(image));
            given(image.getImageKey()).willReturn("img-key");

            // when
            postService.deletePost(POST_ID, AUTH_MEMBER);

            // then
            then(s3StorageService).should().delete("img-key");
            then(imageRepository).should().deleteByPostId(POST_ID);
        }
    }

    @Nested
    @DisplayName("Presigned URL 발급")
    class GeneratePresignedUrl {

        @Test
        @DisplayName("이미지 contentType이면 Presigned URL을 반환한다")
        void success() {
            // given
            PostPresignedUrlRequest request = new PostPresignedUrlRequest("photo.jpg", "image/jpeg", 1024L);
            PresignedUrlResponse expected = new PresignedUrlResponse("https://s3.example.com/upload", "post-images/uuid.jpg");
            given(s3StorageService.generatePresignedUrl(any(UploadType.class), any(), any(), any(Long.class)))
                    .willReturn(expected);

            // when
            PresignedUrlResponse response = postService.generatePostPresignedUrl(request);

            // then
            assertThat(response.url()).isEqualTo(expected.url());
            assertThat(response.key()).isEqualTo(expected.key());
        }

        @Test
        @DisplayName("image/로 시작하지 않는 contentType이면 INVALID_CONTENT_TYPE 예외가 발생한다")
        void invalidContentType() {
            // given
            PostPresignedUrlRequest request = new PostPresignedUrlRequest("file.pdf", "application/pdf", 1024L);

            // when & then
            assertThatThrownBy(() -> postService.generatePostPresignedUrl(request))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_CONTENT_TYPE));
        }
    }
}
