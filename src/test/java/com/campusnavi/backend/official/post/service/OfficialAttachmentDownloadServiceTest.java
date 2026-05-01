package com.campusnavi.backend.official.post.service;

import com.campusnavi.backend.global.common.AuthContext;
import com.campusnavi.backend.global.exception.BusinessException;
import com.campusnavi.backend.global.exception.ErrorCode;
import com.campusnavi.backend.infra.storage.S3StorageService;
import com.campusnavi.backend.official.post.dto.AttachmentDownloadResponse;
import com.campusnavi.backend.official.post.entity.OfficialAttachment;
import com.campusnavi.backend.official.post.entity.OfficialAttachmentDownload;
import com.campusnavi.backend.official.post.entity.OfficialPost;
import com.campusnavi.backend.official.post.repository.OfficialAttachmentDownloadRepository;
import com.campusnavi.backend.official.post.repository.OfficialAttachmentRepository;
import com.campusnavi.backend.official.post.repository.OfficialPostRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class OfficialAttachmentDownloadServiceTest {

    @Mock
    private OfficialPostRepository postRepository;

    @Mock
    private OfficialAttachmentRepository attachmentRepository;

    @Mock
    private OfficialAttachmentDownloadRepository downloadRepository;

    @Mock
    private S3StorageService storageService;

    @InjectMocks
    private OfficialAttachmentDownloadService officialAttachmentDownloadService;

    private static final Long MEMBER_ID = 1L;
    private static final Long UNIVERSITY_ID = 10L;
    private static final Long POST_ID = 100L;
    private static final Long ATTACHMENT_ID = 500L;
    private static final AuthContext CONTEXT = new AuthContext(MEMBER_ID, UNIVERSITY_ID);

    @Nested
    @DisplayName("다운로드 URL 발급")
    class IssueDownloadUrl {

        @Test
        @DisplayName("정상 요청이면 presigned URL을 발급하고 이력을 적재한다")
        void success() {
            // given
            OfficialAttachment attachment = mockAttachment(POST_ID, "doc.pdf", "official/doc.pdf");
            given(postRepository.existsActiveByIdAndUniversityScope(POST_ID, UNIVERSITY_ID)).willReturn(true);
            given(attachmentRepository.findById(ATTACHMENT_ID)).willReturn(Optional.of(attachment));
            given(storageService.generateGetPresignedUrl(eq("official/doc.pdf"), eq("doc.pdf"), any(Duration.class)))
                    .willReturn("https://signed.example/doc.pdf");

            // when
            AttachmentDownloadResponse response = officialAttachmentDownloadService.issueDownloadUrl(
                    POST_ID, ATTACHMENT_ID, CONTEXT);

            // then
            assertThat(response.downloadUrl()).isEqualTo("https://signed.example/doc.pdf");
            assertThat(response.expiresInSeconds()).isEqualTo(600L);
            then(downloadRepository).should().save(any(OfficialAttachmentDownload.class));
        }

        @Test
        @DisplayName("존재하지 않거나 비활성화/스코프 밖 공지이면 OFFICIAL_POST_NOT_FOUND 예외가 발생한다")
        void postNotFound() {
            // given
            given(postRepository.existsActiveByIdAndUniversityScope(POST_ID, UNIVERSITY_ID)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> officialAttachmentDownloadService.issueDownloadUrl(POST_ID, ATTACHMENT_ID, CONTEXT))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.OFFICIAL_POST_NOT_FOUND));
            then(attachmentRepository).should(never()).findById(any());
            then(downloadRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("존재하지 않는 첨부이면 OFFICIAL_ATTACHMENT_NOT_FOUND 예외가 발생한다")
        void attachmentNotFound() {
            // given
            given(postRepository.existsActiveByIdAndUniversityScope(POST_ID, UNIVERSITY_ID)).willReturn(true);
            given(attachmentRepository.findById(ATTACHMENT_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> officialAttachmentDownloadService.issueDownloadUrl(POST_ID, ATTACHMENT_ID, CONTEXT))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.OFFICIAL_ATTACHMENT_NOT_FOUND));
            then(downloadRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("첨부의 post가 요청 postId와 다르면 OFFICIAL_ATTACHMENT_NOT_FOUND 예외가 발생한다")
        void attachmentBelongsToOtherPost() {
            // given
            OfficialAttachment attachment = mockAttachment(999L, "doc.pdf", "official/doc.pdf");
            given(postRepository.existsActiveByIdAndUniversityScope(POST_ID, UNIVERSITY_ID)).willReturn(true);
            given(attachmentRepository.findById(ATTACHMENT_ID)).willReturn(Optional.of(attachment));

            // when & then
            assertThatThrownBy(() -> officialAttachmentDownloadService.issueDownloadUrl(POST_ID, ATTACHMENT_ID, CONTEXT))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.OFFICIAL_ATTACHMENT_NOT_FOUND));
            then(downloadRepository).should(never()).save(any());
        }
    }

    private OfficialAttachment mockAttachment(Long ownerPostId, String originalName, String s3Key) {
        OfficialAttachment attachment = mock(OfficialAttachment.class);
        OfficialPost post = mock(OfficialPost.class);
        lenient().when(post.getId()).thenReturn(ownerPostId);
        lenient().when(attachment.getPost()).thenReturn(post);
        lenient().when(attachment.getOriginalName()).thenReturn(originalName);
        lenient().when(attachment.getS3Key()).thenReturn(s3Key);
        return attachment;
    }
}
