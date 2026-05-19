package com.campusnavi.backend.official.post.service;

import com.campusnavi.backend.global.common.AuthContext;
import com.campusnavi.backend.global.exception.BusinessException;
import com.campusnavi.backend.global.exception.ErrorCode;
import com.campusnavi.backend.official.post.dto.FolderScrapResponse;
import com.campusnavi.backend.official.post.dto.OfficialPostScrapFolderResponse;
import com.campusnavi.backend.official.post.dto.RecentScrapResponse;
import com.campusnavi.backend.official.post.dto.ScrapBulkDeleteResponse;
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

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
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
            then(scrapFolderRepository).should().decrementScrapCount(FOLDER_A);
            then(scrapFolderRepository).should().incrementScrapCount(FOLDER_B);
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
            then(scrapFolderRepository).should().decrementScrapCount(FOLDER_A);
            then(scrapFolderRepository).should(never()).incrementScrapCount(any());
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
            then(scrapFolderRepository).should(never()).incrementScrapCount(any());
            then(scrapFolderRepository).should(never()).decrementScrapCount(any());
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

    @Nested
    @DisplayName("폴더 스크랩 목록 조회")
    class GetFolderScraps {

        @Test
        @DisplayName("본인 폴더면 스크랩 목록을 반환한다")
        void success() {
            // given
            given(scrapFolderRepository.findByIdAndMemberId(FOLDER_A, MEMBER_ID))
                    .willReturn(Optional.of(mock(ScrapFolder.class)));
            List<FolderScrapResponse> expected = List.of(
                    new FolderScrapResponse(1L, 2L, "공지", "장학", null, null, true));
            given(scrapRepository.findFolderScraps(MEMBER_ID, FOLDER_A)).willReturn(expected);

            // when
            List<FolderScrapResponse> result = officialPostScrapService.getFolderScraps(FOLDER_A, CONTEXT);

            // then
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("존재하지 않거나 타인 폴더면 SCRAP_FOLDER_NOT_FOUND 예외가 발생한다")
        void notFound() {
            // given
            given(scrapFolderRepository.findByIdAndMemberId(FOLDER_A, MEMBER_ID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> officialPostScrapService.getFolderScraps(FOLDER_A, CONTEXT))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.SCRAP_FOLDER_NOT_FOUND));
            then(scrapRepository).should(never()).findFolderScraps(any(), any());
        }
    }

    @Nested
    @DisplayName("최근 스크랩 조회")
    class GetRecentScraps {

        @Test
        @DisplayName("최신순 8건을 post 중복 없이 반환한다")
        void success() {
            // given
            given(scrapRepository.findRecentScrappedPostIds(eq(MEMBER_ID), any()))
                    .willReturn(List.of(3L, 1L, 2L));
            RecentScrapResponse card1 = new RecentScrapResponse(1L, "공지1", "장학", null, null);
            RecentScrapResponse card2 = new RecentScrapResponse(2L, "공지2", "취업", null, null);
            RecentScrapResponse card3 = new RecentScrapResponse(3L, "공지3", "행사", null, null);
            given(scrapRepository.findRecentScrapCards(List.of(3L, 1L, 2L)))
                    .willReturn(List.of(card1, card2, card3));

            // when
            List<RecentScrapResponse> result = officialPostScrapService.getRecentScraps(MEMBER_ID);

            // then
            assertThat(result).containsExactly(card3, card1, card2);
        }

        @Test
        @DisplayName("스크랩이 없으면 빈 목록을 반환한다")
        void empty() {
            // given
            given(scrapRepository.findRecentScrappedPostIds(eq(MEMBER_ID), any()))
                    .willReturn(List.of());

            // when
            List<RecentScrapResponse> result = officialPostScrapService.getRecentScraps(MEMBER_ID);

            // then
            assertThat(result).isEmpty();
            then(scrapRepository).should(never()).findRecentScrapCards(any());
        }
    }

    private OfficialPost activePost(Long postId) {
        OfficialPost post = mock(OfficialPost.class);
        given(post.getId()).willReturn(postId);
        return post;
    }

    @Nested
    @DisplayName("스크랩 다중 제거")
    class DeleteScraps {

        @Test
        @DisplayName("선택한 스크랩을 삭제하고 실제 건수와 삭제된 postId를 반환한다")
        void success() {
            // given
            given(scrapFolderRepository.findByIdAndMemberId(FOLDER_A, MEMBER_ID))
                    .willReturn(Optional.of(mock(ScrapFolder.class)));
            given(scrapRepository.findScrappedPostIds(any(), eq(MEMBER_ID), eq(FOLDER_A)))
                    .willReturn(List.of(100L, 200L));
            given(scrapRepository.deleteScrapsByIds(any(), eq(MEMBER_ID), eq(FOLDER_A)))
                    .willReturn(2);

            // when
            ScrapBulkDeleteResponse result =
                    officialPostScrapService.deleteScraps(FOLDER_A, List.of(1L, 2L), CONTEXT);

            // then
            assertThat(result.deletedCount()).isEqualTo(2);
            assertThat(result.deletedPostIds()).containsExactly(100L, 200L);
            then(scrapFolderRepository).should().decrementScrapCount(FOLDER_A, 2L);
        }

        @Test
        @DisplayName("일치하는 스크랩이 없으면 0건을 반환하고 삭제하지 않는다")
        void noMatch() {
            // given
            given(scrapFolderRepository.findByIdAndMemberId(FOLDER_A, MEMBER_ID))
                    .willReturn(Optional.of(mock(ScrapFolder.class)));
            given(scrapRepository.findScrappedPostIds(any(), eq(MEMBER_ID), eq(FOLDER_A)))
                    .willReturn(List.of());

            // when
            ScrapBulkDeleteResponse result =
                    officialPostScrapService.deleteScraps(FOLDER_A, List.of(9L), CONTEXT);

            // then
            assertThat(result.deletedCount()).isZero();
            assertThat(result.deletedPostIds()).isEmpty();
            then(scrapRepository).should(never()).deleteScrapsByIds(any(), any(), any());
            then(scrapFolderRepository).should(never()).decrementScrapCount(any(), anyLong());
        }

        @Test
        @DisplayName("존재하지 않거나 타인 폴더면 SCRAP_FOLDER_NOT_FOUND 예외가 발생한다")
        void folderNotFound() {
            // given
            given(scrapFolderRepository.findByIdAndMemberId(FOLDER_A, MEMBER_ID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> officialPostScrapService.deleteScraps(FOLDER_A, List.of(1L), CONTEXT))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.SCRAP_FOLDER_NOT_FOUND));
            then(scrapRepository).should(never()).findScrappedPostIds(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("스크랩 복구")
    class RestoreScraps {

        @Test
        @DisplayName("스크랩 삭제된 공지를 같은 폴더로 재스크랩한다")
        void success() {
            // given
            given(scrapFolderRepository.findByIdAndMemberId(FOLDER_A, MEMBER_ID))
                    .willReturn(Optional.of(mock(ScrapFolder.class)));
            given(scrapRepository.findExistingPostIds(eq(MEMBER_ID), eq(FOLDER_A), any()))
                    .willReturn(List.of());
            List<OfficialPost> posts = List.of(activePost(7L), activePost(8L));
            given(postRepository.findByIdInAndUniversityScope(any(), eq(UNIVERSITY_ID)))
                    .willReturn(posts);

            // when
            officialPostScrapService.restoreScraps(FOLDER_A, List.of(7L, 8L), CONTEXT);

            // then
            then(scrapRepository).should()
                    .saveAll(argThat((Collection<OfficialPostScrap> c) -> c.size() == 2));
            then(scrapFolderRepository).should().incrementScrapCount(FOLDER_A, 2L);
        }

        @Test
        @DisplayName("이미 폴더에 있는 공지는 건너뛴다")
        void skipExisting() {
            // given
            given(scrapFolderRepository.findByIdAndMemberId(FOLDER_A, MEMBER_ID))
                    .willReturn(Optional.of(mock(ScrapFolder.class)));
            given(scrapRepository.findExistingPostIds(eq(MEMBER_ID), eq(FOLDER_A), any()))
                    .willReturn(List.of(7L));
            List<OfficialPost> posts = List.of(activePost(7L), activePost(8L));
            given(postRepository.findByIdInAndUniversityScope(any(), eq(UNIVERSITY_ID)))
                    .willReturn(posts);

            // when
            officialPostScrapService.restoreScraps(FOLDER_A, List.of(7L, 8L), CONTEXT);

            // then
            then(scrapRepository).should()
                    .saveAll(argThat((Collection<OfficialPostScrap> c) -> c.size() == 1));
            then(scrapFolderRepository).should().incrementScrapCount(FOLDER_A, 1L);
        }

        @Test
        @DisplayName("스코프 밖이거나 완전 삭제된 공지는 건너뛴다")
        void skipMissing() {
            // given
            given(scrapFolderRepository.findByIdAndMemberId(FOLDER_A, MEMBER_ID))
                    .willReturn(Optional.of(mock(ScrapFolder.class)));
            given(scrapRepository.findExistingPostIds(eq(MEMBER_ID), eq(FOLDER_A), any()))
                    .willReturn(List.of());
            given(postRepository.findByIdInAndUniversityScope(any(), eq(UNIVERSITY_ID)))
                    .willReturn(List.of());

            // when
            officialPostScrapService.restoreScraps(FOLDER_A, List.of(7L), CONTEXT);

            // then
            then(scrapRepository).should(never()).saveAll(any());
            then(scrapFolderRepository).should(never()).incrementScrapCount(any(), anyLong());
        }

        @Test
        @DisplayName("존재하지 않거나 타인 폴더면 SCRAP_FOLDER_NOT_FOUND 예외가 발생한다")
        void folderNotFound() {
            // given
            given(scrapFolderRepository.findByIdAndMemberId(FOLDER_A, MEMBER_ID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> officialPostScrapService.restoreScraps(FOLDER_A, List.of(7L), CONTEXT))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.SCRAP_FOLDER_NOT_FOUND));
            then(scrapRepository).should(never()).saveAll(any());
        }
    }
}
