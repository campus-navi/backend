package com.campusnavi.backend.scrap.service;

import com.campusnavi.backend.global.exception.BusinessException;
import com.campusnavi.backend.global.exception.ErrorCode;
import com.campusnavi.backend.scrap.dto.ScrapFolderCreateRequest;
import com.campusnavi.backend.scrap.dto.ScrapFolderResponse;
import com.campusnavi.backend.scrap.dto.ScrapFolderSort;
import com.campusnavi.backend.scrap.dto.ScrapFolderUpdateRequest;
import com.campusnavi.backend.scrap.entity.ScrapFolder;
import com.campusnavi.backend.scrap.repository.ScrapFolderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ScrapFolderServiceTest {

    @Mock
    private ScrapFolderRepository scrapFolderRepository;

    @InjectMocks
    private ScrapFolderService scrapFolderService;

    private static final Long MEMBER_ID = 1L;
    private static final Long FOLDER_ID = 100L;

    @Nested
    @DisplayName("폴더 생성")
    class Create {

        @Test
        @DisplayName("이름이 중복되지 않으면 저장한다")
        void success() {
            // given
            ScrapFolderCreateRequest request = new ScrapFolderCreateRequest("취업", "취업 공고");
            given(scrapFolderRepository.existsByMemberIdAndName(MEMBER_ID, "취업")).willReturn(false);

            // when
            assertThatCode(() -> scrapFolderService.create(MEMBER_ID, request))
                    .doesNotThrowAnyException();

            // then
            then(scrapFolderRepository).should().save(any(ScrapFolder.class));
        }

        @Test
        @DisplayName("이름이 중복되면 SCRAP_FOLDER_NAME_DUPLICATE 예외가 발생한다")
        void duplicateName() {
            // given
            ScrapFolderCreateRequest request = new ScrapFolderCreateRequest("취업", null);
            given(scrapFolderRepository.existsByMemberIdAndName(MEMBER_ID, "취업")).willReturn(true);

            // when & then
            assertThatThrownBy(() -> scrapFolderService.create(MEMBER_ID, request))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.SCRAP_FOLDER_NAME_DUPLICATE));
            then(scrapFolderRepository).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("폴더 수정")
    class Update {

        @Test
        @DisplayName("본인 폴더면 이름·설명을 수정한다")
        void success() {
            // given
            ScrapFolderUpdateRequest request = new ScrapFolderUpdateRequest("장학", "장학 공지");
            ScrapFolder folder = ScrapFolder.create(MEMBER_ID, "취업", null);
            given(scrapFolderRepository.findByIdAndMemberId(FOLDER_ID, MEMBER_ID))
                    .willReturn(Optional.of(folder));
            given(scrapFolderRepository.existsByMemberIdAndName(MEMBER_ID, "장학")).willReturn(false);

            // when
            scrapFolderService.update(MEMBER_ID, FOLDER_ID, request);

            // then
            assertThat(folder.getName()).isEqualTo("장학");
            assertThat(folder.getDescription()).isEqualTo("장학 공지");
        }

        @Test
        @DisplayName("이름을 바꾸지 않으면 중복 검사를 하지 않는다")
        void sameNameSkipsDuplicateCheck() {
            // given
            ScrapFolderUpdateRequest request = new ScrapFolderUpdateRequest("취업", "수정된 설명");
            ScrapFolder folder = ScrapFolder.create(MEMBER_ID, "취업", null);
            given(scrapFolderRepository.findByIdAndMemberId(FOLDER_ID, MEMBER_ID))
                    .willReturn(Optional.of(folder));

            // when
            assertThatCode(() -> scrapFolderService.update(MEMBER_ID, FOLDER_ID, request))
                    .doesNotThrowAnyException();

            // then
            then(scrapFolderRepository).should(never()).existsByMemberIdAndName(any(), any());
        }

        @Test
        @DisplayName("존재하지 않거나 타인 폴더면 SCRAP_FOLDER_NOT_FOUND 예외가 발생한다")
        void notFound() {
            // given
            ScrapFolderUpdateRequest request = new ScrapFolderUpdateRequest("장학", null);
            given(scrapFolderRepository.findByIdAndMemberId(FOLDER_ID, MEMBER_ID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> scrapFolderService.update(MEMBER_ID, FOLDER_ID, request))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.SCRAP_FOLDER_NOT_FOUND));
        }

        @Test
        @DisplayName("다른 폴더와 이름이 중복되면 SCRAP_FOLDER_NAME_DUPLICATE 예외가 발생한다")
        void duplicateName() {
            // given
            ScrapFolderUpdateRequest request = new ScrapFolderUpdateRequest("장학", null);
            ScrapFolder folder = ScrapFolder.create(MEMBER_ID, "취업", null);
            given(scrapFolderRepository.findByIdAndMemberId(FOLDER_ID, MEMBER_ID))
                    .willReturn(Optional.of(folder));
            given(scrapFolderRepository.existsByMemberIdAndName(MEMBER_ID, "장학")).willReturn(true);

            // when & then
            assertThatThrownBy(() -> scrapFolderService.update(MEMBER_ID, FOLDER_ID, request))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.SCRAP_FOLDER_NAME_DUPLICATE));
        }
    }

    @Nested
    @DisplayName("폴더 삭제")
    class Delete {

        @Test
        @DisplayName("본인 폴더면 삭제한다")
        void success() {
            // given
            ScrapFolder folder = ScrapFolder.create(MEMBER_ID, "취업", null);
            given(scrapFolderRepository.findByIdAndMemberId(FOLDER_ID, MEMBER_ID))
                    .willReturn(Optional.of(folder));

            // when
            assertThatCode(() -> scrapFolderService.delete(MEMBER_ID, FOLDER_ID))
                    .doesNotThrowAnyException();

            // then
            then(scrapFolderRepository).should().delete(folder);
        }

        @Test
        @DisplayName("존재하지 않거나 타인 폴더면 SCRAP_FOLDER_NOT_FOUND 예외가 발생한다")
        void notFound() {
            // given
            given(scrapFolderRepository.findByIdAndMemberId(FOLDER_ID, MEMBER_ID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> scrapFolderService.delete(MEMBER_ID, FOLDER_ID))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.SCRAP_FOLDER_NOT_FOUND));
            then(scrapFolderRepository).should(never()).delete(any());
        }
    }

    @Nested
    @DisplayName("폴더 목록 조회")
    class GetFolders {

        @Test
        @DisplayName("정렬 조건으로 조회하고 폴더의 scrapCount를 반환한다")
        void success() {
            // given
            ScrapFolder folder = mock(ScrapFolder.class);
            given(folder.getId()).willReturn(100L);
            given(folder.getName()).willReturn("취업");
            given(folder.getDescription()).willReturn(null);
            given(folder.getScrapCount()).willReturn(5L);
            given(scrapFolderRepository.findByMemberId(eq(MEMBER_ID), any(Sort.class)))
                    .willReturn(List.of(folder));

            // when
            List<ScrapFolderResponse> result = scrapFolderService.getFolders(MEMBER_ID, ScrapFolderSort.NAME_ASC);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().scrapCount()).isEqualTo(5L);
        }
    }
}
