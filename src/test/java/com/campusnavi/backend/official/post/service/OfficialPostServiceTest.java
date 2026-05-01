package com.campusnavi.backend.official.post.service;

import com.campusnavi.backend.global.common.ProcessingStatus;
import com.campusnavi.backend.global.exception.BusinessException;
import com.campusnavi.backend.global.exception.ErrorCode;
import com.campusnavi.backend.infra.storage.S3StorageService;
import com.campusnavi.backend.official.post.dto.OfficialPostDetailResponse;
import com.campusnavi.backend.official.post.entity.ApplyMethodType;
import com.campusnavi.backend.official.post.entity.OfficialAttachment;
import com.campusnavi.backend.official.post.entity.OfficialPost;
import com.campusnavi.backend.official.post.entity.OfficialPostAiMeta;
import com.campusnavi.backend.official.post.repository.OfficialAttachmentRepository;
import com.campusnavi.backend.official.post.repository.OfficialPostAiMetaRepository;
import com.campusnavi.backend.official.post.repository.OfficialPostRepository;
import com.campusnavi.backend.tag.entity.Tag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class OfficialPostServiceTest {

    @Mock
    private OfficialPostRepository postRepository;

    @Mock
    private OfficialPostAiMetaRepository aiMetaRepository;

    @Mock
    private OfficialAttachmentRepository attachmentRepository;

    @Mock
    private S3StorageService storageService;

    @InjectMocks
    private OfficialPostService officialPostService;

    private static final Long POST_ID = 100L;

    @Nested
    @DisplayName("공식 공지 상세 조회")
    class GetDetail {

        @Test
        @DisplayName("정상 요청이면 공지/AI메타/첨부 정보를 합쳐 응답한다")
        void success() {
            // given
            OfficialPost post = mockPost();
            OfficialPostAiMeta meta = mockMeta(mockTag("장학금"));
            OfficialAttachment image = mockAttachment("img.png", "img/a.png", true);
            OfficialAttachment file = mockAttachment("doc.pdf", "file/b.pdf", false);

            given(postRepository.findByIdAndIsActiveTrue(POST_ID)).willReturn(Optional.of(post));
            given(aiMetaRepository.findByOfficialPostIdWithTag(POST_ID, ProcessingStatus.DONE))
                    .willReturn(Optional.of(meta));
            given(attachmentRepository.findByPostIdOrderBySortOrderAsc(POST_ID))
                    .willReturn(List.of(image, file));
            given(storageService.resolveUrl("img/a.png")).willReturn("https://cdn/img/a.png");
            given(storageService.resolveUrl("file/b.pdf")).willReturn("https://cdn/file/b.pdf");

            // when
            OfficialPostDetailResponse response = officialPostService.getDetail(POST_ID);

            // then
            assertThat(response.postId()).isEqualTo(POST_ID);
            assertThat(response.title()).isEqualTo("제목");
            assertThat(response.publisher()).isEqualTo("학사팀");
            assertThat(response.tagName()).isEqualTo("장학금");
            assertThat(response.summary()).isEqualTo("요약");
            assertThat(response.contentHtml()).isEqualTo("<p>본문</p>");
            assertThat(response.isApplicable()).isTrue();
            assertThat(response.applyMethodType()).isEqualTo(ApplyMethodType.EMAIL);
            assertThat(response.thumbnailUrl()).isEqualTo("https://cdn/img/a.png");
            assertThat(response.attachments()).hasSize(1);
            assertThat(response.attachments().getFirst().name()).isEqualTo("doc.pdf");
            assertThat(response.attachments().getFirst().url()).isEqualTo("https://cdn/file/b.pdf");
        }

        @Test
        @DisplayName("존재하지 않거나 비활성화된 공지이면 OFFICIAL_POST_NOT_FOUND 예외가 발생한다")
        void postNotFound() {
            // given
            given(postRepository.findByIdAndIsActiveTrue(POST_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> officialPostService.getDetail(POST_ID))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.OFFICIAL_POST_NOT_FOUND));
        }

        @Test
        @DisplayName("AI 후처리가 완료되지 않았으면 OFFICIAL_POST_NOT_READY 예외가 발생한다")
        void metaNotReady() {
            // given
            OfficialPost post = mockPost();
            given(postRepository.findByIdAndIsActiveTrue(POST_ID)).willReturn(Optional.of(post));
            given(aiMetaRepository.findByOfficialPostIdWithTag(POST_ID, ProcessingStatus.DONE))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> officialPostService.getDetail(POST_ID))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.OFFICIAL_POST_NOT_READY));
        }

        @Test
        @DisplayName("이미지 첨부가 없으면 thumbnailUrl이 null이다")
        void noImageAttachment() {
            // given
            OfficialPost post = mockPost();
            OfficialPostAiMeta meta = mockMeta(mockTag("장학금"));
            OfficialAttachment file = mockAttachment("doc.pdf", "file/b.pdf", false);

            given(postRepository.findByIdAndIsActiveTrue(POST_ID)).willReturn(Optional.of(post));
            given(aiMetaRepository.findByOfficialPostIdWithTag(POST_ID, ProcessingStatus.DONE))
                    .willReturn(Optional.of(meta));
            given(attachmentRepository.findByPostIdOrderBySortOrderAsc(POST_ID))
                    .willReturn(List.of(file));
            given(storageService.resolveUrl("file/b.pdf")).willReturn("https://cdn/file/b.pdf");

            // when
            OfficialPostDetailResponse response = officialPostService.getDetail(POST_ID);

            // then
            assertThat(response.thumbnailUrl()).isNull();
            assertThat(response.attachments()).hasSize(1);
        }

        @Test
        @DisplayName("비이미지 첨부가 없으면 attachments는 빈 리스트이다")
        void noFileAttachment() {
            // given
            OfficialPost post = mockPost();
            OfficialPostAiMeta meta = mockMeta(mockTag("장학금"));
            OfficialAttachment image = mockAttachment("img.png", "img/a.png", true);

            given(postRepository.findByIdAndIsActiveTrue(POST_ID)).willReturn(Optional.of(post));
            given(aiMetaRepository.findByOfficialPostIdWithTag(POST_ID, ProcessingStatus.DONE))
                    .willReturn(Optional.of(meta));
            given(attachmentRepository.findByPostIdOrderBySortOrderAsc(POST_ID))
                    .willReturn(List.of(image));
            given(storageService.resolveUrl("img/a.png")).willReturn("https://cdn/img/a.png");

            // when
            OfficialPostDetailResponse response = officialPostService.getDetail(POST_ID);

            // then
            assertThat(response.thumbnailUrl()).isEqualTo("https://cdn/img/a.png");
            assertThat(response.attachments()).isEmpty();
        }

        @Test
        @DisplayName("첨부파일이 전혀 없으면 thumbnailUrl은 null이고 attachments는 빈 리스트이다")
        void noAttachment() {
            // given
            OfficialPost post = mockPost();
            OfficialPostAiMeta meta = mockMeta(mockTag("장학금"));

            given(postRepository.findByIdAndIsActiveTrue(POST_ID)).willReturn(Optional.of(post));
            given(aiMetaRepository.findByOfficialPostIdWithTag(POST_ID, ProcessingStatus.DONE))
                    .willReturn(Optional.of(meta));
            given(attachmentRepository.findByPostIdOrderBySortOrderAsc(POST_ID)).willReturn(List.of());

            // when
            OfficialPostDetailResponse response = officialPostService.getDetail(POST_ID);

            // then
            assertThat(response.thumbnailUrl()).isNull();
            assertThat(response.attachments()).isEmpty();
        }

        @Test
        @DisplayName("AI 메타에 tag가 null이면 tagName도 null이다")
        void nullTag() {
            // given
            OfficialPost post = mockPost();
            OfficialPostAiMeta meta = mockMeta(null);

            given(postRepository.findByIdAndIsActiveTrue(POST_ID)).willReturn(Optional.of(post));
            given(aiMetaRepository.findByOfficialPostIdWithTag(POST_ID, ProcessingStatus.DONE))
                    .willReturn(Optional.of(meta));
            given(attachmentRepository.findByPostIdOrderBySortOrderAsc(POST_ID)).willReturn(List.of());

            // when
            OfficialPostDetailResponse response = officialPostService.getDetail(POST_ID);

            // then
            assertThat(response.tagName()).isNull();
        }
    }

    private OfficialPost mockPost() {
        OfficialPost post = mock(OfficialPost.class);
        lenient().when(post.getId()).thenReturn(POST_ID);
        lenient().when(post.getTitle()).thenReturn("제목");
        lenient().when(post.getPublisher()).thenReturn("학사팀");
        lenient().when(post.getSourceUrl()).thenReturn("https://example.com/notice/1");
        lenient().when(post.getPublishedAt()).thenReturn(LocalDate.of(2026, 4, 1));
        lenient().when(post.getHtmlContent()).thenReturn("<p>본문</p>");
        return post;
    }

    private OfficialPostAiMeta mockMeta(Tag tag) {
        OfficialPostAiMeta meta = mock(OfficialPostAiMeta.class);
        lenient().when(meta.getTag()).thenReturn(tag);
        lenient().when(meta.getSummary()).thenReturn("요약");
        lenient().when(meta.isApplicable()).thenReturn(true);
        lenient().when(meta.getStartDate()).thenReturn(LocalDate.of(2026, 4, 1));
        lenient().when(meta.getStartTime()).thenReturn(LocalTime.of(9, 0));
        lenient().when(meta.getEndDate()).thenReturn(LocalDate.of(2026, 5, 31));
        lenient().when(meta.getEndTime()).thenReturn(null);
        lenient().when(meta.getEligibility()).thenReturn("재학생");
        lenient().when(meta.getApplyMethodType()).thenReturn(ApplyMethodType.EMAIL);
        lenient().when(meta.getApplyMethodDetail()).thenReturn(null);
        lenient().when(meta.getRequiredDocuments()).thenReturn("성적증명서");
        lenient().when(meta.getContactPhone()).thenReturn("02-1234-5678");
        lenient().when(meta.getContactEmail()).thenReturn("staff@example.com");
        return meta;
    }

    private Tag mockTag(String name) {
        Tag tag = mock(Tag.class);
        lenient().when(tag.getName()).thenReturn(name);
        return tag;
    }

    private OfficialAttachment mockAttachment(String originalName, String s3Key, boolean isImage) {
        OfficialAttachment attachment = mock(OfficialAttachment.class);
        lenient().when(attachment.getOriginalName()).thenReturn(originalName);
        lenient().when(attachment.getS3Key()).thenReturn(s3Key);
        lenient().when(attachment.isImage()).thenReturn(isImage);
        return attachment;
    }
}
