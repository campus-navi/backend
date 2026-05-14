package com.campusnavi.backend.notification.scheduler;

import com.campusnavi.backend.member.entity.Member;
import com.campusnavi.backend.member.entity.MemberRole;
import com.campusnavi.backend.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class ActivityNotificationSnapshotSchedulerTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ActivityNotificationSnapshotWriter writer;

    @InjectMocks
    private ActivityNotificationSnapshotScheduler scheduler;

    @Nested
    @DisplayName("dispatch")
    class Dispatch {

        @Test
        @DisplayName("청크별로 writer를 호출하고 마지막에 cleanup을 수행한다")
        void chunkAndCleanup() {
            // given
            List<Member> chunk = List.of(member(1L), member(2L));
            given(memberRepository.findActiveAfterIdExcludingRole(
                    eq(0L), eq(MemberRole.ADMIN), any(Pageable.class)))
                    .willReturn(chunk);
            given(memberRepository.findActiveAfterIdExcludingRole(
                    eq(2L), eq(MemberRole.ADMIN), any(Pageable.class)))
                    .willReturn(List.of());

            // when
            scheduler.dispatch();

            // then
            then(writer).should(times(1)).writeChunk(eq(chunk), any(LocalDate.class));
            then(writer).should().cleanupOlderThan(any(LocalDate.class));
        }

        @Test
        @DisplayName("청크에서 예외가 발생해도 다음 청크를 계속 처리한다")
        void chunkFailureContinues() {
            // given
            List<Member> chunkA = List.of(member(1L));
            List<Member> chunkB = List.of(member(2L));
            given(memberRepository.findActiveAfterIdExcludingRole(
                    eq(0L), eq(MemberRole.ADMIN), any(Pageable.class)))
                    .willReturn(chunkA);
            given(memberRepository.findActiveAfterIdExcludingRole(
                    eq(1L), eq(MemberRole.ADMIN), any(Pageable.class)))
                    .willReturn(chunkB);
            given(memberRepository.findActiveAfterIdExcludingRole(
                    eq(2L), eq(MemberRole.ADMIN), any(Pageable.class)))
                    .willReturn(List.of());
            willThrow(new RuntimeException("boom"))
                    .given(writer).writeChunk(eq(chunkA), any(LocalDate.class));

            // when
            scheduler.dispatch();

            // then
            then(writer).should().writeChunk(eq(chunkA), any(LocalDate.class));
            then(writer).should().writeChunk(eq(chunkB), any(LocalDate.class));
            then(writer).should().cleanupOlderThan(any(LocalDate.class));
        }
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
