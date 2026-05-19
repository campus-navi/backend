package com.campusnavi.backend.mypage.service;

import com.campusnavi.backend.mypage.dto.MyScrapResponse;
import com.campusnavi.backend.official.post.dto.RecentScrapResponse;
import com.campusnavi.backend.official.post.service.OfficialPostScrapService;
import com.campusnavi.backend.scrap.dto.ScrapFolderResponse;
import com.campusnavi.backend.scrap.dto.ScrapFolderSort;
import com.campusnavi.backend.scrap.service.ScrapFolderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class MyScrapServiceTest {

    @Mock
    private OfficialPostScrapService officialPostScrapService;

    @Mock
    private ScrapFolderService scrapFolderService;

    @InjectMocks
    private MyScrapService service;

    private static final Long MEMBER_ID = 7L;

    @Nested
    @DisplayName("내 스크랩 화면 조회")
    class GetMyScraps {

        @Test
        @DisplayName("최근 스크랩과 폴더 목록을 조립해 반환한다")
        void success() {
            List<RecentScrapResponse> recent = List.of(
                    new RecentScrapResponse(2L, "장학 공고", "장학", null, null));
            List<ScrapFolderResponse> folders = List.of(
                    new ScrapFolderResponse(100L, "취업", null, 1L));
            given(officialPostScrapService.getRecentScraps(MEMBER_ID)).willReturn(recent);
            given(scrapFolderService.getFolders(MEMBER_ID, ScrapFolderSort.RECENT_SAVED))
                    .willReturn(folders);

            MyScrapResponse result = service.getMyScraps(MEMBER_ID);

            assertThat(result.recentScraps()).isEqualTo(recent);
            assertThat(result.folders()).isEqualTo(folders);
        }

        @Test
        @DisplayName("최근 스크랩과 폴더가 없으면 빈 목록을 반환한다")
        void empty() {
            given(officialPostScrapService.getRecentScraps(MEMBER_ID)).willReturn(List.of());
            given(scrapFolderService.getFolders(MEMBER_ID, ScrapFolderSort.RECENT_SAVED))
                    .willReturn(List.of());

            MyScrapResponse result = service.getMyScraps(MEMBER_ID);

            assertThat(result.recentScraps()).isEmpty();
            assertThat(result.folders()).isEmpty();
        }
    }
}
