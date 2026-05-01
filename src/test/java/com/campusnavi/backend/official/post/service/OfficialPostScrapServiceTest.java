package com.campusnavi.backend.official.post.service;

import com.campusnavi.backend.global.common.AuthContext;
import com.campusnavi.backend.global.exception.BusinessException;
import com.campusnavi.backend.global.exception.ErrorCode;
import com.campusnavi.backend.official.post.entity.OfficialPost;
import com.campusnavi.backend.official.post.entity.OfficialPostScrap;
import com.campusnavi.backend.official.post.repository.OfficialPostRepository;
import com.campusnavi.backend.official.post.repository.OfficialPostScrapRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class OfficialPostScrapServiceTest {

    @Mock
    private OfficialPostRepository postRepository;

    @Mock
    private OfficialPostScrapRepository scrapRepository;

    @InjectMocks
    private OfficialPostScrapService officialPostScrapService;

    private static final Long MEMBER_ID = 1L;
    private static final Long POST_ID = 100L;
    private static final Long UNIVERSITY_ID = 10L;
    private static final AuthContext CONTEXT = new AuthContext(MEMBER_ID, UNIVERSITY_ID);

    @Nested
    @DisplayName("스크랩 추가")
    class Scrap {

        @Test
        @DisplayName("스크랩이 없으면 새로 저장한다")
        void success() {
            // given
            OfficialPost post = mock(OfficialPost.class);
            given(postRepository.findActiveByIdAndUniversityScope(POST_ID, UNIVERSITY_ID))
                    .willReturn(Optional.of(post));
            given(scrapRepository.existsByMemberIdAndPostId(MEMBER_ID, POST_ID)).willReturn(false);

            // when
            assertThatCode(() -> officialPostScrapService.scrap(POST_ID, CONTEXT))
                    .doesNotThrowAnyException();

            // then
            then(scrapRepository).should().save(any(OfficialPostScrap.class));
        }

        @Test
        @DisplayName("이미 스크랩 상태이면 멱등하게 아무 작업도 하지 않는다")
        void idempotent() {
            // given
            OfficialPost post = mock(OfficialPost.class);
            given(postRepository.findActiveByIdAndUniversityScope(POST_ID, UNIVERSITY_ID))
                    .willReturn(Optional.of(post));
            given(scrapRepository.existsByMemberIdAndPostId(MEMBER_ID, POST_ID)).willReturn(true);

            // when
            assertThatCode(() -> officialPostScrapService.scrap(POST_ID, CONTEXT))
                    .doesNotThrowAnyException();

            // then
            then(scrapRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("존재하지 않거나 비활성화/스코프 밖 공지이면 OFFICIAL_POST_NOT_FOUND 예외가 발생한다")
        void postNotFound() {
            // given
            given(postRepository.findActiveByIdAndUniversityScope(POST_ID, UNIVERSITY_ID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> officialPostScrapService.scrap(POST_ID, CONTEXT))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.OFFICIAL_POST_NOT_FOUND));
            then(scrapRepository).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("스크랩 해제")
    class Unscrap {

        @Test
        @DisplayName("스크랩이 있으면 삭제한다")
        void success() {
            // given
            OfficialPostScrap existing = mock(OfficialPostScrap.class);
            given(postRepository.existsActiveByIdAndUniversityScope(POST_ID, UNIVERSITY_ID)).willReturn(true);
            given(scrapRepository.findByMemberIdAndPostId(MEMBER_ID, POST_ID)).willReturn(Optional.of(existing));

            // when
            assertThatCode(() -> officialPostScrapService.unscrap(POST_ID, CONTEXT))
                    .doesNotThrowAnyException();

            // then
            then(scrapRepository).should().delete(existing);
        }

        @Test
        @DisplayName("스크랩이 없어도 멱등하게 200으로 응답한다")
        void idempotent() {
            // given
            given(postRepository.existsActiveByIdAndUniversityScope(POST_ID, UNIVERSITY_ID)).willReturn(true);
            given(scrapRepository.findByMemberIdAndPostId(MEMBER_ID, POST_ID)).willReturn(Optional.empty());

            // when
            assertThatCode(() -> officialPostScrapService.unscrap(POST_ID, CONTEXT))
                    .doesNotThrowAnyException();

            // then
            then(scrapRepository).should(never()).delete(any());
        }

        @Test
        @DisplayName("존재하지 않거나 비활성화/스코프 밖 공지이면 OFFICIAL_POST_NOT_FOUND 예외가 발생한다")
        void postNotFound() {
            // given
            given(postRepository.existsActiveByIdAndUniversityScope(POST_ID, UNIVERSITY_ID)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> officialPostScrapService.unscrap(POST_ID, CONTEXT))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.OFFICIAL_POST_NOT_FOUND));
            then(scrapRepository).should(never()).delete(any());
        }
    }
}
