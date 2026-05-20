package com.campusnavi.backend.official.post.service;

import com.campusnavi.backend.global.common.AuthContext;
import com.campusnavi.backend.global.exception.BusinessException;
import com.campusnavi.backend.global.exception.ErrorCode;
import com.campusnavi.backend.official.post.dto.RemindBulkDeleteResponse;
import com.campusnavi.backend.official.post.entity.OfficialPost;
import com.campusnavi.backend.official.post.entity.OfficialPostAiMeta;
import com.campusnavi.backend.official.post.entity.OfficialPostNotification;
import com.campusnavi.backend.official.post.repository.OfficialPostAiMetaRepository;
import com.campusnavi.backend.official.post.repository.OfficialPostNotificationRepository;
import com.campusnavi.backend.official.post.repository.OfficialPostRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class OfficialPostNotificationServiceTest {

    @Mock
    private OfficialPostRepository postRepository;

    @Mock
    private OfficialPostNotificationRepository notificationRepository;

    @Mock
    private OfficialPostAiMetaRepository aiMetaRepository;

    @InjectMocks
    private OfficialPostNotificationService officialPostNotificationService;

    private void givenEndDate(LocalDate endDate) {
        OfficialPostAiMeta aiMeta = mock(OfficialPostAiMeta.class);
        given(aiMeta.getEndDate()).willReturn(endDate);
        given(aiMetaRepository.findByOfficialPostId(POST_ID)).willReturn(Optional.of(aiMeta));
    }

    private static final Long MEMBER_ID = 1L;
    private static final Long POST_ID = 100L;
    private static final Long UNIVERSITY_ID = 10L;
    private static final AuthContext CONTEXT = new AuthContext(MEMBER_ID, UNIVERSITY_ID);

    @Nested
    @DisplayName("알림 켜기")
    class Enable {

        @Test
        @DisplayName("알림이 켜져 있지 않으면 새로 저장한다")
        void success() {
            // given
            OfficialPost post = mock(OfficialPost.class);
            given(postRepository.findActiveByIdAndUniversityScope(POST_ID, UNIVERSITY_ID))
                    .willReturn(Optional.of(post));
            givenEndDate(LocalDate.now().plusDays(1));
            given(notificationRepository.existsByMemberIdAndPostId(MEMBER_ID, POST_ID)).willReturn(false);

            // when
            assertThatCode(() -> officialPostNotificationService.enable(POST_ID, CONTEXT))
                    .doesNotThrowAnyException();

            // then
            then(notificationRepository).should().save(any(OfficialPostNotification.class));
        }

        @Test
        @DisplayName("마감기한이 오늘이면 정상 저장한다")
        void successWhenDeadlineIsToday() {
            // given
            OfficialPost post = mock(OfficialPost.class);
            given(postRepository.findActiveByIdAndUniversityScope(POST_ID, UNIVERSITY_ID))
                    .willReturn(Optional.of(post));
            givenEndDate(LocalDate.now());
            given(notificationRepository.existsByMemberIdAndPostId(MEMBER_ID, POST_ID)).willReturn(false);

            // when
            assertThatCode(() -> officialPostNotificationService.enable(POST_ID, CONTEXT))
                    .doesNotThrowAnyException();

            // then
            then(notificationRepository).should().save(any(OfficialPostNotification.class));
        }

        @Test
        @DisplayName("이미 알림이 켜져 있으면 멱등하게 아무 작업도 하지 않는다")
        void idempotent() {
            // given
            OfficialPost post = mock(OfficialPost.class);
            given(postRepository.findActiveByIdAndUniversityScope(POST_ID, UNIVERSITY_ID))
                    .willReturn(Optional.of(post));
            givenEndDate(LocalDate.now().plusDays(1));
            given(notificationRepository.existsByMemberIdAndPostId(MEMBER_ID, POST_ID)).willReturn(true);

            // when
            assertThatCode(() -> officialPostNotificationService.enable(POST_ID, CONTEXT))
                    .doesNotThrowAnyException();

            // then
            then(notificationRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("마감기한이 없으면 OFFICIAL_POST_DEADLINE_REQUIRED 예외가 발생한다")
        void deadlineNull() {
            // given
            OfficialPost post = mock(OfficialPost.class);
            given(postRepository.findActiveByIdAndUniversityScope(POST_ID, UNIVERSITY_ID))
                    .willReturn(Optional.of(post));
            givenEndDate(null);

            // when & then
            assertThatThrownBy(() -> officialPostNotificationService.enable(POST_ID, CONTEXT))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.OFFICIAL_POST_DEADLINE_REQUIRED));
            then(notificationRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("AI 메타가 없으면 OFFICIAL_POST_DEADLINE_REQUIRED 예외가 발생한다")
        void aiMetaNotFound() {
            // given
            OfficialPost post = mock(OfficialPost.class);
            given(postRepository.findActiveByIdAndUniversityScope(POST_ID, UNIVERSITY_ID))
                    .willReturn(Optional.of(post));
            given(aiMetaRepository.findByOfficialPostId(POST_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> officialPostNotificationService.enable(POST_ID, CONTEXT))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.OFFICIAL_POST_DEADLINE_REQUIRED));
            then(notificationRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("마감기한이 이미 지났으면 OFFICIAL_POST_DEADLINE_REQUIRED 예외가 발생한다")
        void deadlinePassed() {
            // given
            OfficialPost post = mock(OfficialPost.class);
            given(postRepository.findActiveByIdAndUniversityScope(POST_ID, UNIVERSITY_ID))
                    .willReturn(Optional.of(post));
            givenEndDate(LocalDate.now().minusDays(1));

            // when & then
            assertThatThrownBy(() -> officialPostNotificationService.enable(POST_ID, CONTEXT))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.OFFICIAL_POST_DEADLINE_REQUIRED));
            then(notificationRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("존재하지 않거나 비활성화/스코프 밖 공지이면 OFFICIAL_POST_NOT_FOUND 예외가 발생한다")
        void postNotFound() {
            // given
            given(postRepository.findActiveByIdAndUniversityScope(POST_ID, UNIVERSITY_ID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> officialPostNotificationService.enable(POST_ID, CONTEXT))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.OFFICIAL_POST_NOT_FOUND));
            then(notificationRepository).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("알림 끄기")
    class Disable {

        @Test
        @DisplayName("알림이 켜져 있으면 삭제한다")
        void success() {
            // given
            OfficialPostNotification existing = mock(OfficialPostNotification.class);
            given(postRepository.existsActiveByIdAndUniversityScope(POST_ID, UNIVERSITY_ID)).willReturn(true);
            given(notificationRepository.findByMemberIdAndPostId(MEMBER_ID, POST_ID))
                    .willReturn(Optional.of(existing));

            // when
            assertThatCode(() -> officialPostNotificationService.disable(POST_ID, CONTEXT))
                    .doesNotThrowAnyException();

            // then
            then(notificationRepository).should().delete(existing);
        }

        @Test
        @DisplayName("알림이 꺼져 있어도 멱등하게 아무 작업도 하지 않는다")
        void idempotent() {
            // given
            given(postRepository.existsActiveByIdAndUniversityScope(POST_ID, UNIVERSITY_ID)).willReturn(true);
            given(notificationRepository.findByMemberIdAndPostId(MEMBER_ID, POST_ID))
                    .willReturn(Optional.empty());

            // when
            assertThatCode(() -> officialPostNotificationService.disable(POST_ID, CONTEXT))
                    .doesNotThrowAnyException();

            // then
            then(notificationRepository).should(never()).delete(any());
        }

        @Test
        @DisplayName("존재하지 않거나 비활성화/스코프 밖 공지이면 OFFICIAL_POST_NOT_FOUND 예외가 발생한다")
        void postNotFound() {
            // given
            given(postRepository.existsActiveByIdAndUniversityScope(POST_ID, UNIVERSITY_ID)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> officialPostNotificationService.disable(POST_ID, CONTEXT))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.OFFICIAL_POST_NOT_FOUND));
            then(notificationRepository).should(never()).delete(any());
        }
    }

    @Nested
    @DisplayName("리마인드 다중 제거")
    class DeleteReminds {

        @Test
        @DisplayName("보유한 리마인드만 제거하고 건수와 제거된 postId를 반환한다")
        void success() {
            given(notificationRepository.findExistingPostIds(eq(MEMBER_ID), any()))
                    .willReturn(List.of(100L, 200L));
            given(notificationRepository.deleteByMemberIdAndPostIdIn(eq(MEMBER_ID), any()))
                    .willReturn(2);

            RemindBulkDeleteResponse result =
                    officialPostNotificationService.deleteReminds(List.of(100L, 200L), CONTEXT);

            assertThat(result.deletedCount()).isEqualTo(2);
            assertThat(result.deletedPostIds()).containsExactly(100L, 200L);
        }

        @Test
        @DisplayName("보유한 리마인드가 없으면 0건을 반환하고 삭제하지 않는다")
        void none() {
            given(notificationRepository.findExistingPostIds(eq(MEMBER_ID), any()))
                    .willReturn(List.of());

            RemindBulkDeleteResponse result =
                    officialPostNotificationService.deleteReminds(List.of(100L), CONTEXT);

            assertThat(result.deletedCount()).isZero();
            assertThat(result.deletedPostIds()).isEmpty();
            then(notificationRepository).should(never()).deleteByMemberIdAndPostIdIn(any(), any());
        }
    }

    @Nested
    @DisplayName("리마인드 복구")
    class RestoreReminds {

        @Test
        @DisplayName("스코프 내 공지의 리마인드를 재등록한다")
        void success() {
            List<OfficialPost> posts = List.of(post(7L), post(8L));
            given(notificationRepository.findExistingPostIds(eq(MEMBER_ID), any()))
                    .willReturn(List.of());
            given(postRepository.findByIdInAndUniversityScope(any(), eq(UNIVERSITY_ID)))
                    .willReturn(posts);

            officialPostNotificationService.restoreReminds(List.of(7L, 8L), CONTEXT);

            then(notificationRepository).should()
                    .saveAll(argThat((Collection<OfficialPostNotification> c) -> c.size() == 2));
        }

        @Test
        @DisplayName("이미 설정된 공지는 건너뛴다")
        void skipExisting() {
            List<OfficialPost> posts = List.of(post(7L), post(8L));
            given(notificationRepository.findExistingPostIds(eq(MEMBER_ID), any()))
                    .willReturn(List.of(7L));
            given(postRepository.findByIdInAndUniversityScope(any(), eq(UNIVERSITY_ID)))
                    .willReturn(posts);

            officialPostNotificationService.restoreReminds(List.of(7L, 8L), CONTEXT);

            then(notificationRepository).should()
                    .saveAll(argThat((Collection<OfficialPostNotification> c) -> c.size() == 1));
        }

        @Test
        @DisplayName("스코프 밖이거나 없는 공지만 남으면 저장하지 않는다")
        void skipMissing() {
            given(notificationRepository.findExistingPostIds(eq(MEMBER_ID), any()))
                    .willReturn(List.of());
            given(postRepository.findByIdInAndUniversityScope(any(), eq(UNIVERSITY_ID)))
                    .willReturn(List.of());

            officialPostNotificationService.restoreReminds(List.of(7L, 8L), CONTEXT);

            then(notificationRepository).should(never()).saveAll(any());
        }

        private OfficialPost post(Long id) {
            OfficialPost post = mock(OfficialPost.class);
            given(post.getId()).willReturn(id);
            return post;
        }
    }
}
