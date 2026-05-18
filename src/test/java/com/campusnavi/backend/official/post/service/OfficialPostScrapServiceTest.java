package com.campusnavi.backend.official.post.service;

import com.campusnavi.backend.global.common.AuthContext;
import com.campusnavi.backend.global.exception.BusinessException;
import com.campusnavi.backend.global.exception.ErrorCode;
import com.campusnavi.backend.official.post.dto.OfficialPostScrapFolderResponse;
import com.campusnavi.backend.official.post.entity.OfficialPost;
import com.campusnavi.backend.official.post.entity.OfficialPostScrap;
import com.campusnavi.backend.official.post.repository.OfficialPostRepository;
import com.campusnavi.backend.official.post.repository.OfficialPostScrapRepository;
import com.campusnavi.backend.scrap.entity.ScrapFolder;
import com.campusnavi.backend.scrap.repository.ScrapFolderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

    @Mock
    private ScrapFolderRepository scrapFolderRepository;

    @InjectMocks
    private OfficialPostScrapService officialPostScrapService;

    private static final Long MEMBER_ID = 1L;
    private static final Long POST_ID = 100L;
    private static final Long UNIVERSITY_ID = 10L;
    private static final Long FOLDER_A = 11L;
    private static final Long FOLDER_B = 22L;
    private static final AuthContext CONTEXT = new AuthContext(MEMBER_ID, UNIVERSITY_ID);

    private OfficialPostScrap scrapIn(Long folderId) {
        OfficialPostScrap scrap = mock(OfficialPostScrap.class);
        given(scrap.getScrapFolderId()).willReturn(folderId);
        return scrap;
    }

    @Nested
    @DisplayName("스크랩 폴더 설정")
    class SetScrapFolders {

        @Test
        @DisplayName("새로 선택된 폴더에는 추가하고 빠진 폴더에서는 제거한다")
        void addAndRemove() {
            // given
            OfficialPost post = mock(OfficialPost.class);
            given(postRepository.findActiveByIdAndUniversityScope(POST_ID, UNIVERSITY_ID))
                    .willReturn(Optional.of(post));
            given(scrapFolderRepository.findAllByIdInAndMemberId(any(), eq(MEMBER_ID)))
                    .willReturn(List.of(mock(ScrapFolder.class)));
            OfficialPostScrap scrapA = scrapIn(FOLDER_A);
            given(scrapRepository.findByMemberIdAndPostId(MEMBER_ID, POST_ID))
                    .willReturn(List.of(scrapA));

            // when
            officialPostScrapService.setScrapFolders(POST_ID, List.of(FOLDER_B), CONTEXT);

            // then
            then(scrapRepository).should().delete(scrapA);
            then(scrapRepository).should().save(any(OfficialPostScrap.class));
        }

        @Test
        @DisplayName("빈 폴더 집합이면 모든 스크랩을 제거한다")
        void empty() {
            // given
            OfficialPost post = mock(OfficialPost.class);
            given(postRepository.findActiveByIdAndUniversityScope(POST_ID, UNIVERSITY_ID))
                    .willReturn(Optional.of(post));
            OfficialPostScrap scrapA = scrapIn(FOLDER_A);
            given(scrapRepository.findByMemberIdAndPostId(MEMBER_ID, POST_ID))
                    .willReturn(List.of(scrapA));

            // when
            officialPostScrapService.setScrapFolders(POST_ID, List.of(), CONTEXT);

            // then
            then(scrapRepository).should().delete(scrapA);
            then(scrapRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("동일한 폴더 집합이면 멱등하게 추가·제거하지 않는다")
        void idempotent() {
            // given
            OfficialPost post = mock(OfficialPost.class);
            given(postRepository.findActiveByIdAndUniversityScope(POST_ID, UNIVERSITY_ID))
                    .willReturn(Optional.of(post));
            given(scrapFolderRepository.findAllByIdInAndMemberId(any(), eq(MEMBER_ID)))
                    .willReturn(List.of(mock(ScrapFolder.class)));
            OfficialPostScrap scrapA = scrapIn(FOLDER_A);
            given(scrapRepository.findByMemberIdAndPostId(MEMBER_ID, POST_ID))
                    .willReturn(List.of(scrapA));

            // when
            officialPostScrapService.setScrapFolders(POST_ID, List.of(FOLDER_A), CONTEXT);

            // then
            then(scrapRepository).should(never()).save(any());
            then(scrapRepository).should(never()).delete(any());
        }

        @Test
        @DisplayName("본인 소유가 아니거나 존재하지 않는 폴더가 포함되면 SCRAP_FOLDER_NOT_FOUND 예외가 발생한다")
        void folderNotFound() {
            // given
            OfficialPost post = mock(OfficialPost.class);
            given(postRepository.findActiveByIdAndUniversityScope(POST_ID, UNIVERSITY_ID))
                    .willReturn(Optional.of(post));
            given(scrapFolderRepository.findAllByIdInAndMemberId(any(), eq(MEMBER_ID)))
                    .willReturn(List.of());

            // when & then
            assertThatThrownBy(() -> officialPostScrapService.setScrapFolders(POST_ID, List.of(FOLDER_A), CONTEXT))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.SCRAP_FOLDER_NOT_FOUND));
            then(scrapRepository).should(never()).save(any());
            then(scrapRepository).should(never()).delete(any());
        }

        @Test
        @DisplayName("존재하지 않거나 스코프 밖 공지이면 OFFICIAL_POST_NOT_FOUND 예외가 발생한다")
        void postNotFound() {
            // given
            given(postRepository.findActiveByIdAndUniversityScope(POST_ID, UNIVERSITY_ID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> officialPostScrapService.setScrapFolders(POST_ID, List.of(FOLDER_A), CONTEXT))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.OFFICIAL_POST_NOT_FOUND));
            then(scrapRepository).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("스크랩 폴더 목록 조회")
    class GetScrapFolders {

        @Test
        @DisplayName("전체 폴더와 해당 공지의 담김 여부를 반환한다")
        void success() {
            // given
            given(postRepository.existsActiveByIdAndUniversityScope(POST_ID, UNIVERSITY_ID)).willReturn(true);
            given(scrapRepository.findScrapFolderIdsByMemberIdAndPostId(MEMBER_ID, POST_ID))
                    .willReturn(List.of(FOLDER_A));
            ScrapFolder folderA = mock(ScrapFolder.class);
            given(folderA.getId()).willReturn(FOLDER_A);
            given(folderA.getName()).willReturn("폴더A");
            ScrapFolder folderB = mock(ScrapFolder.class);
            given(folderB.getId()).willReturn(FOLDER_B);
            given(folderB.getName()).willReturn("폴더B");
            given(scrapFolderRepository.findByMemberId(eq(MEMBER_ID), any()))
                    .willReturn(List.of(folderA, folderB));

            // when
            List<OfficialPostScrapFolderResponse> result =
                    officialPostScrapService.getScrapFolders(POST_ID, CONTEXT);

            // then
            assertThat(result).containsExactly(
                    new OfficialPostScrapFolderResponse(FOLDER_A, "폴더A", true),
                    new OfficialPostScrapFolderResponse(FOLDER_B, "폴더B", false));
        }

        @Test
        @DisplayName("존재하지 않거나 스코프 밖 공지이면 OFFICIAL_POST_NOT_FOUND 예외가 발생한다")
        void postNotFound() {
            // given
            given(postRepository.existsActiveByIdAndUniversityScope(POST_ID, UNIVERSITY_ID)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> officialPostScrapService.getScrapFolders(POST_ID, CONTEXT))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.OFFICIAL_POST_NOT_FOUND));
        }
    }
}
