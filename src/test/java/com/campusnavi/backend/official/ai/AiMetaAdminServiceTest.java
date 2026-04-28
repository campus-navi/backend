package com.campusnavi.backend.official.ai;

import com.campusnavi.backend.global.common.ProcessingStatus;
import com.campusnavi.backend.infra.ai.AiClient;
import com.campusnavi.backend.official.ai.dto.*;
import com.campusnavi.backend.official.ai.service.AiMetaAdminService;
import com.campusnavi.backend.official.ai.service.AiMetaProcessor;
import com.campusnavi.backend.official.ai.service.AiMetaService;
import com.campusnavi.backend.official.post.entity.OfficialPost;
import com.campusnavi.backend.official.post.entity.OfficialPostAiMeta;
import com.campusnavi.backend.official.post.repository.OfficialPostAiMetaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class AiMetaAdminServiceTest {

    @Mock
    private OfficialPostAiMetaRepository metaRepository;

    @Mock
    private AiClient aiClient;

    @Mock
    private AiMetaProcessor processor;

    @Mock
    private AiMetaService aiMetaService;

    @InjectMocks
    private AiMetaAdminService service;

    @Nested
    @DisplayName("PENDING 배치 처리")
    class ProcessPendingBatch {

        @Test
        @DisplayName("PENDING 항목이 없으면 AI 호출 없이 BatchResult(0, 0)을 반환한다")
        void noPending() {
            // given
            given(metaRepository.findAllByStatus(eq(ProcessingStatus.PENDING), any(PageRequest.class)))
                    .willReturn(new PageImpl<>(List.of()));

            // when
            BatchResult result = service.processPendingBatch(100);

            // then
            assertThat(result.success()).isZero();
            assertThat(result.failure()).isZero();
            then(aiClient).shouldHaveNoInteractions();
            then(processor).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("성공 항목에는 saveResult를, 실패 항목에는 markFailed를 호출한다")
        void mixedResults() {
            // given
            OfficialPost post1 = mock(OfficialPost.class);
            OfficialPost post2 = mock(OfficialPost.class);

            OfficialPostAiMeta meta1 = mock(OfficialPostAiMeta.class);
            OfficialPostAiMeta meta2 = mock(OfficialPostAiMeta.class);
            given(meta1.getOfficialPost()).willReturn(post1);
            given(meta2.getOfficialPost()).willReturn(post2);

            given(metaRepository.findAllByStatus(eq(ProcessingStatus.PENDING), any(PageRequest.class)))
                    .willReturn(new PageImpl<>(List.of(meta1, meta2)));
            given(processor.buildRequests(anyList()))
                    .willReturn(List.of(
                            new OfficialAiRequest(1L, "본문1", List.of(), List.of()),
                            new OfficialAiRequest(2L, "본문2", List.of(), List.of())
                    ));

            OfficialAiResponse response = mock(OfficialAiResponse.class);
            given(aiClient.post(anyString(), any(), eq(OfficialAiBatchResponse.class)))
                    .willReturn(new OfficialAiBatchResponse(List.of(
                            new OfficialAiBatchItemResponse(1L, true, null, response),
                            new OfficialAiBatchItemResponse(2L, false, "AI 처리 오류", null)
                    )));

            // when
            BatchResult result = service.processPendingBatch(100);

            // then
            assertThat(result.success()).isEqualTo(1L);
            assertThat(result.failure()).isEqualTo(1L);
            then(aiMetaService).should().saveResult(1L, response);
            then(aiMetaService).should().markFailed(2L, "AI 처리 오류");
        }

        @Test
        @DisplayName("전체 성공 시 failure는 0이고 markFailed를 호출하지 않는다")
        void allSuccess() {
            // given
            OfficialPost post = mock(OfficialPost.class);
            OfficialPostAiMeta meta = mock(OfficialPostAiMeta.class);
            given(meta.getOfficialPost()).willReturn(post);

            given(metaRepository.findAllByStatus(eq(ProcessingStatus.PENDING), any(PageRequest.class)))
                    .willReturn(new PageImpl<>(List.of(meta)));
            given(processor.buildRequests(anyList()))
                    .willReturn(List.of(new OfficialAiRequest(1L, "본문", List.of(), List.of())));

            OfficialAiResponse response = mock(OfficialAiResponse.class);
            given(aiClient.post(anyString(), any(), eq(OfficialAiBatchResponse.class)))
                    .willReturn(new OfficialAiBatchResponse(List.of(
                            new OfficialAiBatchItemResponse(1L, true, null, response)
                    )));

            // when
            BatchResult result = service.processPendingBatch(100);

            // then
            assertThat(result.success()).isEqualTo(1L);
            assertThat(result.failure()).isZero();
            then(aiMetaService).should(never()).markFailed(anyLong(), anyString());
        }

        @Test
        @DisplayName("전체 실패 시 success는 0이고 saveResult를 호출하지 않는다")
        void allFailure() {
            // given
            OfficialPost post = mock(OfficialPost.class);
            OfficialPostAiMeta meta = mock(OfficialPostAiMeta.class);
            given(meta.getOfficialPost()).willReturn(post);

            given(metaRepository.findAllByStatus(eq(ProcessingStatus.PENDING), any(PageRequest.class)))
                    .willReturn(new PageImpl<>(List.of(meta)));
            given(processor.buildRequests(anyList()))
                    .willReturn(List.of(new OfficialAiRequest(1L, "본문", List.of(), List.of())));

            given(aiClient.post(anyString(), any(), eq(OfficialAiBatchResponse.class)))
                    .willReturn(new OfficialAiBatchResponse(List.of(
                            new OfficialAiBatchItemResponse(1L, false, "처리 불가", null)
                    )));

            // when
            BatchResult result = service.processPendingBatch(100);

            // then
            assertThat(result.success()).isZero();
            assertThat(result.failure()).isEqualTo(1L);
            then(aiMetaService).should(never()).saveResult(anyLong(), any());
        }

        @Test
        @DisplayName("limit 파라미터가 Page 조회 크기로 전달된다")
        void limitAsPageSize() {
            // given
            given(metaRepository.findAllByStatus(eq(ProcessingStatus.PENDING), eq(PageRequest.of(0, 50))))
                    .willReturn(new PageImpl<>(List.of()));

            // when
            service.processPendingBatch(50);

            // then
            then(metaRepository).should().findAllByStatus(ProcessingStatus.PENDING, PageRequest.of(0, 50));
        }
    }
}
