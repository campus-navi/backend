package com.campusnavi.backend.official.post.recommend.service;

import com.campusnavi.backend.official.post.recommend.entity.FeedRecommendSnapshot;
import com.campusnavi.backend.official.post.recommend.repository.FeedRecommendSnapshotRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class RecommendSnapshotWriterTest {

    @Mock
    private FeedRecommendSnapshotRepository snapshotRepository;

    @InjectMocks
    private RecommendSnapshotWriter writer;

    private static final Long MEMBER_ID = 1L;
    private static final LocalDateTime SLOT_AT = LocalDateTime.of(2026, 5, 12, 9, 0);

    @Nested
    @DisplayName("persist")
    class Persist {

        @Test
        @DisplayName("입력 값으로 스냅샷을 저장한다")
        void save() {
            // given
            List<Long> ids = List.of(7L, 3L);

            // when
            writer.persist(MEMBER_ID, SLOT_AT, ids);

            // then
            ArgumentCaptor<FeedRecommendSnapshot> captor = ArgumentCaptor.forClass(FeedRecommendSnapshot.class);
            then(snapshotRepository).should().save(captor.capture());
            FeedRecommendSnapshot saved = captor.getValue();
            assertThat(saved.getMemberId()).isEqualTo(MEMBER_ID);
            assertThat(saved.getSlotAt()).isEqualTo(SLOT_AT);
            assertThat(saved.getPostIds()).containsExactly(7L, 3L);
        }

        @Test
        @DisplayName("빈 ids도 그대로 저장한다")
        void saveEmpty() {
            // when
            writer.persist(MEMBER_ID, SLOT_AT, List.of());

            // then
            ArgumentCaptor<FeedRecommendSnapshot> captor = ArgumentCaptor.forClass(FeedRecommendSnapshot.class);
            then(snapshotRepository).should().save(captor.capture());
            assertThat(captor.getValue().getPostIds()).isEmpty();
        }
    }
}
