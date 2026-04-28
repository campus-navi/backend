package com.campusnavi.backend.official.ai;

import com.campusnavi.backend.infra.ai.AiClient;
import com.campusnavi.backend.infra.storage.S3StorageService;
import com.campusnavi.backend.official.ai.dto.OfficialAiRequest;
import com.campusnavi.backend.official.ai.dto.OfficialAiResponse;
import com.campusnavi.backend.official.ai.service.AiMetaProcessor;
import com.campusnavi.backend.official.ai.service.AiMetaService;
import com.campusnavi.backend.official.domain.entity.OfficialAttachment;
import com.campusnavi.backend.official.domain.entity.OfficialPost;
import com.campusnavi.backend.official.domain.repository.OfficialAttachmentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class AiMetaProcessorTest {

    @Mock
    private OfficialAttachmentRepository attachmentRepository;

    @Mock
    private S3StorageService s3StorageService;

    @Mock
    private AiClient aiClient;

    @Mock
    private AiMetaService aiMetaService;

    @InjectMocks
    private AiMetaProcessor processor;

    private static final Long POST_ID = 1L;
    private static final String STRUCTURED_TEXT = "본문 텍스트";

    @Nested
    @DisplayName("정상 처리")
    class Success {

        @Test
        @DisplayName("이미지와 일반 첨부파일을 분리하여 AI 요청을 보낸다")
        void imageAndFileAttachments() {
            // given
            OfficialPost post = mock(OfficialPost.class);
            given(post.getId()).willReturn(POST_ID);
            given(post.getStructuredText()).willReturn(STRUCTURED_TEXT);

            OfficialAttachment image = OfficialAttachment.create(post, "image.png", "img/a.png", "image/png", true, (short) 0);
            OfficialAttachment file = OfficialAttachment.create(post, "file.pdf", "file/b.pdf", "application/pdf", false, (short) 1);

            given(attachmentRepository.findByPostId(POST_ID)).willReturn(List.of(image, file));
            given(s3StorageService.resolveUrl("img/a.png")).willReturn("https://cdn/img/a.png");
            given(s3StorageService.resolveUrl("file/b.pdf")).willReturn("https://cdn/file/b.pdf");
            given(aiClient.post(anyString(), any(), eq(OfficialAiResponse.class))).willReturn(mock(OfficialAiResponse.class));

            // when
            processor.process(post);

            // then
            ArgumentCaptor<OfficialAiRequest> captor = ArgumentCaptor.forClass(OfficialAiRequest.class);
            then(aiClient).should().post(anyString(), captor.capture(), eq(OfficialAiResponse.class));

            OfficialAiRequest request = captor.getValue();
            assertThat(request.imageUrls()).containsExactly("https://cdn/img/a.png");
            assertThat(request.attachmentUrls()).containsExactly("https://cdn/file/b.pdf");
        }

        @Test
        @DisplayName("첨부파일이 없으면 빈 리스트로 AI 요청을 보낸다")
        void noAttachments() {
            // given
            OfficialPost post = mock(OfficialPost.class);
            given(post.getId()).willReturn(POST_ID);
            given(post.getStructuredText()).willReturn(STRUCTURED_TEXT);

            given(attachmentRepository.findByPostId(POST_ID)).willReturn(List.of());
            given(aiClient.post(anyString(), any(), eq(OfficialAiResponse.class))).willReturn(mock(OfficialAiResponse.class));

            // when
            processor.process(post);

            // then
            ArgumentCaptor<OfficialAiRequest> captor = ArgumentCaptor.forClass(OfficialAiRequest.class);
            then(aiClient).should().post(anyString(), captor.capture(), eq(OfficialAiResponse.class));

            assertThat(captor.getValue().imageUrls()).isEmpty();
            assertThat(captor.getValue().attachmentUrls()).isEmpty();
        }

        @Test
        @DisplayName("AI 응답을 받으면 saveResult를 호출한다")
        void savesResult() {
            // given
            OfficialPost post = mock(OfficialPost.class);
            OfficialAiResponse response = mock(OfficialAiResponse.class);
            given(post.getId()).willReturn(POST_ID);
            given(post.getStructuredText()).willReturn(STRUCTURED_TEXT);

            given(attachmentRepository.findByPostId(POST_ID)).willReturn(List.of());
            given(aiClient.post(anyString(), any(), eq(OfficialAiResponse.class))).willReturn(response);

            // when
            processor.process(post);

            // then
            then(aiMetaService).should().saveResult(POST_ID, response);
            then(aiMetaService).shouldHaveNoMoreInteractions();
        }
    }

    @Nested
    @DisplayName("배치 요청 생성")
    class BuildRequests {

        @Test
        @DisplayName("각 post의 attachment를 postId 기준으로 분배하여 요청을 생성한다")
        void separatesAttachments() {
            // given
            OfficialPost post1 = mock(OfficialPost.class);
            OfficialPost post2 = mock(OfficialPost.class);
            given(post1.getId()).willReturn(1L);
            given(post2.getId()).willReturn(2L);
            given(post1.getStructuredText()).willReturn("본문1");
            given(post2.getStructuredText()).willReturn("본문2");

            OfficialAttachment img1 = OfficialAttachment.create(post1, "img1.png", "img/1.png", "image/png", true, (short) 0);
            OfficialAttachment file2 = OfficialAttachment.create(post2, "file2.pdf", "file/2.pdf", "application/pdf", false, (short) 0);

            given(attachmentRepository.findByPostIdIn(List.of(1L, 2L))).willReturn(List.of(img1, file2));
            given(s3StorageService.resolveUrl("img/1.png")).willReturn("https://cdn/img/1.png");
            given(s3StorageService.resolveUrl("file/2.pdf")).willReturn("https://cdn/file/2.pdf");

            // when
            List<OfficialAiRequest> requests = processor.buildRequests(List.of(post1, post2));

            // then
            assertThat(requests).hasSize(2);
            assertThat(requests.getFirst().imageUrls()).containsExactly("https://cdn/img/1.png");
            assertThat(requests.getFirst().attachmentUrls()).isEmpty();
            assertThat(requests.get(1).imageUrls()).isEmpty();
            assertThat(requests.get(1).attachmentUrls()).containsExactly("https://cdn/file/2.pdf");
        }

        @Test
        @DisplayName("attachment가 없는 post는 빈 리스트로 요청을 생성한다")
        void noAttachments() {
            // given
            OfficialPost post = mock(OfficialPost.class);
            given(post.getId()).willReturn(1L);
            given(post.getStructuredText()).willReturn("본문");

            given(attachmentRepository.findByPostIdIn(List.of(1L))).willReturn(List.of());

            // when
            List<OfficialAiRequest> requests = processor.buildRequests(List.of(post));

            // then
            assertThat(requests).hasSize(1);
            assertThat(requests.getFirst().imageUrls()).isEmpty();
            assertThat(requests.getFirst().attachmentUrls()).isEmpty();
        }

        @Test
        @DisplayName("post 목록이 비어있으면 빈 리스트를 반환하고 attachment 조회를 수행한다")
        void emptyPosts() {
            // given
            given(attachmentRepository.findByPostIdIn(List.of())).willReturn(List.of());

            // when
            List<OfficialAiRequest> requests = processor.buildRequests(List.of());

            // then
            assertThat(requests).isEmpty();
            then(s3StorageService).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("attachment 조회는 단일 쿼리로 수행한다")
        void singleBatchQuery() {
            // given
            OfficialPost post1 = mock(OfficialPost.class);
            OfficialPost post2 = mock(OfficialPost.class);
            given(post1.getId()).willReturn(1L);
            given(post2.getId()).willReturn(2L);
            given(post1.getStructuredText()).willReturn("본문1");
            given(post2.getStructuredText()).willReturn("본문2");

            given(attachmentRepository.findByPostIdIn(List.of(1L, 2L))).willReturn(List.of());

            // when
            processor.buildRequests(List.of(post1, post2));

            // then
            then(attachmentRepository).should(never()).findByPostId(any());
            then(attachmentRepository).should().findByPostIdIn(List.of(1L, 2L));
        }
    }

    @Nested
    @DisplayName("실패 처리")
    class Failure {

        @Test
        @DisplayName("AI 서버 호출 중 예외가 발생하면 markFailed를 호출한다")
        void aiClientFailure() {
            // given
            OfficialPost post = mock(OfficialPost.class);
            given(post.getId()).willReturn(POST_ID);
            given(post.getStructuredText()).willReturn(STRUCTURED_TEXT);

            given(attachmentRepository.findByPostId(POST_ID)).willReturn(List.of());
            given(aiClient.post(anyString(), any(), eq(OfficialAiResponse.class)))
                    .willThrow(new RuntimeException("AI 서버 오류"));

            // when
            processor.process(post);

            // then
            then(aiMetaService).should().markFailed(eq(POST_ID), contains("AI 서버 오류"));
            then(aiMetaService).shouldHaveNoMoreInteractions();
        }

        @Test
        @DisplayName("첨부파일 조회 중 예외가 발생하면 markFailed를 호출한다")
        void repositoryFailure() {
            // given
            OfficialPost post = mock(OfficialPost.class);
            given(post.getId()).willReturn(POST_ID);

            given(attachmentRepository.findByPostId(POST_ID))
                    .willThrow(new RuntimeException("DB 오류"));

            // when
            processor.process(post);

            // then
            then(aiMetaService).should().markFailed(eq(POST_ID), contains("DB 오류"));
            then(aiMetaService).shouldHaveNoMoreInteractions();
        }
    }
}
