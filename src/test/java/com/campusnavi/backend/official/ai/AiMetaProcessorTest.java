package com.campusnavi.backend.official.ai;

import com.campusnavi.backend.infra.ai.AiClient;
import com.campusnavi.backend.infra.storage.S3StorageService;
import com.campusnavi.backend.official.ai.dto.OfficialAiRequest;
import com.campusnavi.backend.official.ai.dto.OfficialAiResponse;
import com.campusnavi.backend.official.entity.OfficialAttachment;
import com.campusnavi.backend.official.entity.OfficialPost;
import com.campusnavi.backend.official.repository.OfficialAttachmentRepository;
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
        void separatesImageAndFileAttachments() {
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
        void sendsEmptyListsWhenNoAttachments() {
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
        void callsSaveResultOnSuccess() {
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
    @DisplayName("실패 처리")
    class Failure {

        @Test
        @DisplayName("AI 서버 호출 중 예외가 발생하면 markFailed를 호출한다")
        void callsMarkFailedOnAiClientException() {
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
        void callsMarkFailedOnRepositoryException() {
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
