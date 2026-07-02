package com.campusnavi.backend.official.ai;

import com.campusnavi.backend.global.common.ProcessingStatus;
import com.campusnavi.backend.global.exception.BusinessException;
import com.campusnavi.backend.global.exception.ErrorCode;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

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

    @Mock
    private Executor adminTaskExecutor;

    @InjectMocks
    private AiMetaAdminService service;

    private List<OfficialPostAiMeta> mockMetas(int count) {
        List<OfficialPostAiMeta> metas = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            OfficialPostAiMeta meta = mock(OfficialPostAiMeta.class);
            given(meta.getOfficialPost()).willReturn(mock(OfficialPost.class));
            metas.add(meta);
        }
        return metas;
    }

    @Nested
    @DisplayName("PENDING 배치 처리")
    class ProcessPendingBatch {

        @Test
        @DisplayName("PENDING 항목이 없으면 AI 호출 없이 BatchResult(0, 0)을 반환한다")
        void noPending() {
            // given
            given(metaRepository.findPendingBatch(eq(ProcessingStatus.PENDING), isNull(), isNull(), any(PageRequest.class)))
                    .willReturn(List.of());

            // when
            BatchResult result = service.processPendingBatch(100, null, null);

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

            given(metaRepository.findPendingBatch(eq(ProcessingStatus.PENDING), isNull(), isNull(), any(PageRequest.class)))
                    .willReturn(List.of(meta1, meta2));
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
            BatchResult result = service.processPendingBatch(100, null, null);

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

            given(metaRepository.findPendingBatch(eq(ProcessingStatus.PENDING), isNull(), isNull(), any(PageRequest.class)))
                    .willReturn(List.of(meta));
            given(processor.buildRequests(anyList()))
                    .willReturn(List.of(new OfficialAiRequest(1L, "본문", List.of(), List.of())));

            OfficialAiResponse response = mock(OfficialAiResponse.class);
            given(aiClient.post(anyString(), any(), eq(OfficialAiBatchResponse.class)))
                    .willReturn(new OfficialAiBatchResponse(List.of(
                            new OfficialAiBatchItemResponse(1L, true, null, response)
                    )));

            // when
            BatchResult result = service.processPendingBatch(100, null, null);

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

            given(metaRepository.findPendingBatch(eq(ProcessingStatus.PENDING), isNull(), isNull(), any(PageRequest.class)))
                    .willReturn(List.of(meta));
            given(processor.buildRequests(anyList()))
                    .willReturn(List.of(new OfficialAiRequest(1L, "본문", List.of(), List.of())));

            given(aiClient.post(anyString(), any(), eq(OfficialAiBatchResponse.class)))
                    .willReturn(new OfficialAiBatchResponse(List.of(
                            new OfficialAiBatchItemResponse(1L, false, "처리 불가", null)
                    )));

            // when
            BatchResult result = service.processPendingBatch(100, null, null);

            // then
            assertThat(result.success()).isZero();
            assertThat(result.failure()).isEqualTo(1L);
            then(aiMetaService).should(never()).saveResult(anyLong(), any());
        }

        @Test
        @DisplayName("limit 파라미터가 조회 크기로 전달된다")
        void limitAsPageSize() {
            // given
            given(metaRepository.findPendingBatch(eq(ProcessingStatus.PENDING), isNull(), isNull(), eq(PageRequest.of(0, 50))))
                    .willReturn(List.of());

            // when
            service.processPendingBatch(50, null, null);

            // then
            then(metaRepository).should().findPendingBatch(ProcessingStatus.PENDING, null, null, PageRequest.of(0, 50));
        }

        @Test
        @DisplayName("대상이 청크 크기를 넘으면 청크 수만큼 나눠 호출한다")
        void chunked() {
            // given — 12건 → 10건 + 2건
            List<OfficialPostAiMeta> metas = mockMetas(12);
            given(metaRepository.findPendingBatch(eq(ProcessingStatus.PENDING), isNull(), isNull(), any(PageRequest.class)))
                    .willReturn(metas);
            given(processor.buildRequests(anyList())).willReturn(List.of());
            given(aiClient.post(anyString(), any(), eq(OfficialAiBatchResponse.class)))
                    .willReturn(new OfficialAiBatchResponse(List.of()));

            // when
            service.processPendingBatch(100, null, null);

            // then
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<OfficialPost>> captor = ArgumentCaptor.forClass(List.class);
            then(processor).should(times(2)).buildRequests(captor.capture());
            assertThat(captor.getAllValues().get(0)).hasSize(10);
            assertThat(captor.getAllValues().get(1)).hasSize(2);
            then(aiClient).should(times(2)).post(anyString(), any(), eq(OfficialAiBatchResponse.class));
        }

        @Test
        @DisplayName("청크 호출이 실패해도 다음 청크를 계속 처리한다")
        void chunkFailureContinues() {
            // given — 12건, 1번째 청크 호출 실패 / 2번째 청크 성공
            List<OfficialPostAiMeta> metas = mockMetas(12);
            given(metaRepository.findPendingBatch(eq(ProcessingStatus.PENDING), isNull(), isNull(), any(PageRequest.class)))
                    .willReturn(metas);
            given(processor.buildRequests(anyList())).willReturn(List.of());

            OfficialAiResponse response = mock(OfficialAiResponse.class);
            given(aiClient.post(anyString(), any(), eq(OfficialAiBatchResponse.class)))
                    .willThrow(new RuntimeException("연결 실패"))
                    .willReturn(new OfficialAiBatchResponse(List.of(
                            new OfficialAiBatchItemResponse(11L, true, null, response)
                    )));

            // when
            BatchResult result = service.processPendingBatch(100, null, null);

            // then
            assertThat(result.success()).isEqualTo(1L);
            assertThat(result.failure()).isEqualTo(10L);
            then(aiMetaService).should(times(10)).markFailed(any(), anyString());
            then(aiMetaService).should().saveResult(11L, response);
        }
    }

    @Nested
    @DisplayName("비동기 실행 가드")
    class AsyncGuard {

        @Test
        @DisplayName("실행 중 재요청이면 예외가 발생한다")
        void alreadyRunning() {
            // given
            service.startProcessPendingAsync(100, null, null);

            // when & then
            assertThatThrownBy(() -> service.startProcessPendingAsync(100, null, null))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.AI_BATCH_ALREADY_RUNNING));
        }

        @Test
        @DisplayName("작업 완료 후에는 다시 실행할 수 있다")
        void guardReleased() {
            // given
            given(metaRepository.findPendingBatch(eq(ProcessingStatus.PENDING), isNull(), isNull(), any(PageRequest.class)))
                    .willReturn(List.of());
            service.startProcessPendingAsync(100, null, null);
            assertThat(service.status().running()).isTrue();

            ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
            then(adminTaskExecutor).should().execute(captor.capture());

            // when
            captor.getValue().run();

            // then
            assertThat(service.status().running()).isFalse();
            assertThat(service.status().finishedAt()).isNotNull();
            assertThatCode(() -> service.startProcessPendingAsync(100, null, null)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("배치 본체가 예외를 던져도 가드가 해제된다")
        void guardReleasedOnFailure() {
            // given
            given(metaRepository.findPendingBatch(eq(ProcessingStatus.PENDING), isNull(), isNull(), any(PageRequest.class)))
                    .willThrow(new RuntimeException("DB 오류"));
            service.startProcessPendingAsync(100, null, null);

            ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
            then(adminTaskExecutor).should().execute(captor.capture());

            // when
            assertThatCode(() -> captor.getValue().run()).doesNotThrowAnyException();

            // then
            assertThat(service.status().running()).isFalse();
        }
    }

    @Nested
    @DisplayName("상태 조회")
    class Status {

        @Test
        @DisplayName("실행 여부와 상태별 카운트를 반환한다")
        void success() {
            // given
            given(metaRepository.countByStatus(ProcessingStatus.PENDING)).willReturn(5L);
            given(metaRepository.countByStatus(ProcessingStatus.DONE)).willReturn(2L);
            given(metaRepository.countByStatus(ProcessingStatus.FAILED)).willReturn(1L);

            // when
            AiMetaStatusResponse status = service.status();

            // then
            assertThat(status.running()).isFalse();
            assertThat(status.pendingCount()).isEqualTo(5L);
            assertThat(status.doneCount()).isEqualTo(2L);
            assertThat(status.failedCount()).isEqualTo(1L);
        }
    }
}
