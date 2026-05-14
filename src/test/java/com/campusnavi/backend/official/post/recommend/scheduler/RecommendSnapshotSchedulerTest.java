package com.campusnavi.backend.official.post.recommend.scheduler;

import com.campusnavi.backend.member.entity.Member;
import com.campusnavi.backend.member.entity.MemberRole;
import com.campusnavi.backend.member.repository.MemberRepository;
import com.campusnavi.backend.official.post.recommend.service.RecommendSnapshotBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class RecommendSnapshotSchedulerTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private RecommendSnapshotBuilder snapshotBuilder;

    @Mock
    private RecommendSnapshotCleaner cleaner;

    @InjectMocks
    private RecommendSnapshotScheduler scheduler;

    @Nested
    @DisplayName("rebuildAll")
    class RebuildAll {

        @Test
        @DisplayName("활성 USER가 없으면 builder를 호출하지 않는다")
        void noActiveUsers() {
            // given
            given(memberRepository.findActiveAfterIdExcludingRole(
                    eq(0L), eq(MemberRole.ADMIN), any(Pageable.class)))
                    .willReturn(List.of());

            // when
            scheduler.rebuildAll();

            // then
            then(snapshotBuilder).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("청크 페이지네이션으로 모든 활성 USER를 처리한다")
        void chunkPagination() {
            // given
            List<Member> chunk = members(1L, 2L, 3L);
            given(memberRepository.findActiveAfterIdExcludingRole(
                    eq(0L), eq(MemberRole.ADMIN), any(Pageable.class)))
                    .willReturn(chunk);
            given(memberRepository.findActiveAfterIdExcludingRole(
                    eq(3L), eq(MemberRole.ADMIN), any(Pageable.class)))
                    .willReturn(List.of());

            // when
            scheduler.rebuildAll();

            // then
            then(snapshotBuilder).should(times(3)).computeAndUpsert(any(Member.class));
        }

        @Test
        @DisplayName("한 멤버 RuntimeException 시 로깅하고 다음 멤버를 계속 처리한다")
        void singleRuntimeExceptionIgnored() {
            // given
            Member m1 = member(1L);
            Member m2 = member(2L);
            given(memberRepository.findActiveAfterIdExcludingRole(
                    eq(0L), eq(MemberRole.ADMIN), any(Pageable.class)))
                    .willReturn(List.of(m1, m2));
            given(memberRepository.findActiveAfterIdExcludingRole(
                    eq(2L), eq(MemberRole.ADMIN), any(Pageable.class)))
                    .willReturn(List.of());
            willThrow(new RuntimeException("boom"))
                    .given(snapshotBuilder).computeAndUpsert(m1);

            // when
            scheduler.rebuildAll();

            // then
            then(snapshotBuilder).should().computeAndUpsert(m1);
            then(snapshotBuilder).should().computeAndUpsert(m2);
        }

        @Test
        @DisplayName("rebuild 끝에 보관 기간이 지난 스냅샷을 정리한다")
        void cleanupAfterRebuild() {
            // given
            given(memberRepository.findActiveAfterIdExcludingRole(
                    eq(0L), eq(MemberRole.ADMIN), any(Pageable.class)))
                    .willReturn(List.of());

            // when
            scheduler.rebuildAll();

            // then
            then(cleaner).should().cleanupOlderThan(any(LocalDateTime.class));
        }
    }

    private static List<Member> members(Long... ids) {
        return Arrays.stream(ids).map(RecommendSnapshotSchedulerTest::member)
                .toList();
    }

    private static Member member(Long id) {
        Member m = Member.join("u" + id + "@test.ac.kr", "u" + id, "pw", "nick" + id,
                100L, null, 2022, 3);
        try {
            Field idField = Member.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(m, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return m;
    }
}
