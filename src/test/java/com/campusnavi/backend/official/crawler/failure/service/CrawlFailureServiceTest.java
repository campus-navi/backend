package com.campusnavi.backend.official.crawler.failure.service;

import com.campusnavi.backend.official.crawler.config.CrawlProperties;
import com.campusnavi.backend.official.crawler.dto.PostList;
import com.campusnavi.backend.official.crawler.failure.entity.CrawlFailure;
import com.campusnavi.backend.official.crawler.failure.repository.CrawlFailureRepository;
import com.campusnavi.backend.official.source.entity.OfficialSource;
import com.campusnavi.backend.official.source.entity.SourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class CrawlFailureServiceTest {

    @Mock
    private CrawlFailureRepository repository;

    @Mock
    private CrawlFailureRetryHandler retryHandler;

    @Mock
    private CrawlProperties properties;

    @Mock
    private RetryAction action;

    @InjectMocks
    private CrawlFailureService service;

    private OfficialSource source;
    private PostList post;

    @BeforeEach
    void setUp() {
        source = OfficialSource.create(1L, 1L, null, null, SourceType.CRAWL,
                "KU_SEJONG_BASIC", "고려대 세종 공지", "https://test.com/list");
        ReflectionTestUtils.setField(source, "id", 10L);
        post = new PostList("orig-1", "테스트 게시물", "공지팀",
                "https://test.com/view/1", LocalDate.of(2026, 5, 13));
    }

    @Nested
    @DisplayName("실패 기록 (record)")
    class Record {

        @Test
        @DisplayName("기존 실패 row가 없으면 새로 INSERT 한다")
        void insertWhenAbsent() {
            // given
            given(repository.findBySourceIdAndOriginalId(10L, "orig-1")).willReturn(Optional.empty());

            // when
            service.record(source, post, new RuntimeException("크롤 실패"));

            // then
            then(repository).should().save(any(CrawlFailure.class));
        }

        @Test
        @DisplayName("기존 row가 있으면 retryCount는 유지하고 lastError/lastRetriedAt만 갱신한다")
        void touchWhenPresent() {
            // given
            CrawlFailure existing = CrawlFailure.create(source, post, new RuntimeException("이전 오류"));
            given(repository.findBySourceIdAndOriginalId(10L, "orig-1")).willReturn(Optional.of(existing));

            // when
            service.record(source, post, new RuntimeException("새 오류"));

            // then
            assertThat(existing.getRetryCount()).isEqualTo(0);
            assertThat(existing.getLastError()).contains("새 오류");
            assertThat(existing.getLastRetriedAt()).isNotNull();
            then(repository).should(never()).save(any(CrawlFailure.class));
        }
    }

    @Nested
    @DisplayName("재시도 대상 조회 (findRetryTargets)")
    class FindRetryTargets {

        @Test
        @DisplayName("properties 의 max-count 를 그대로 위임 조회한다")
        void delegatesToRepository() {
            // given
            given(properties.retry()).willReturn(new CrawlProperties.Retry(3));
            given(repository.findByRetryCountLessThan(3)).willReturn(List.of());

            // when
            List<CrawlFailure> result = service.findRetryTargets();

            // then
            assertThat(result).isEmpty();
            then(repository).should().findByRetryCountLessThan(3);
        }
    }

    @Nested
    @DisplayName("한 건 재시도 (retryOne)")
    class RetryOne {

        @Test
        @DisplayName("재시도 성공 시 markSuccess 만 호출하고 markFailure 는 호출하지 않는다")
        void successDelegatesToMarkSuccess() throws Exception {
            // given
            CrawlFailure failure = CrawlFailure.create(source, post, new RuntimeException("이전 오류"));
            ReflectionTestUtils.setField(failure, "id", 100L);

            // when
            service.retryOne(failure, action);

            // then
            then(action).should().execute();
            then(retryHandler).should().markSuccess(100L);
            then(retryHandler).should(never()).markFailure(any(), any());
        }

        @Test
        @DisplayName("재시도 실패 시 markFailure 만 호출하고 markSuccess 는 호출하지 않는다")
        void failureDelegatesToMarkFailure() throws Exception {
            // given
            CrawlFailure failure = CrawlFailure.create(source, post, new RuntimeException("이전 오류"));
            ReflectionTestUtils.setField(failure, "id", 100L);
            RuntimeException retryError = new RuntimeException("재시도 실패");
            willThrow(retryError).given(action).execute();

            // when
            service.retryOne(failure, action);

            // then
            then(retryHandler).should().markFailure(eq(100L), eq(retryError));
            then(retryHandler).should(never()).markSuccess(any());
        }
    }
}
