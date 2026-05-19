package com.campusnavi.backend.mypage.service;

import com.campusnavi.backend.mypage.dto.MyScrapResponse;
import com.campusnavi.backend.official.post.dto.RecentScrapResponse;
import com.campusnavi.backend.official.post.service.OfficialPostScrapService;
import com.campusnavi.backend.scrap.dto.ScrapFolderResponse;
import com.campusnavi.backend.scrap.dto.ScrapFolderSort;
import com.campusnavi.backend.scrap.service.ScrapFolderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyScrapService {

    private final OfficialPostScrapService officialPostScrapService;
    private final ScrapFolderService scrapFolderService;

    public MyScrapResponse getMyScraps(Long memberId) {
        List<RecentScrapResponse> recentScraps = officialPostScrapService.getRecentScraps(memberId);
        List<ScrapFolderResponse> folders =
                scrapFolderService.getFolders(memberId, ScrapFolderSort.RECENT_SAVED);
        return new MyScrapResponse(recentScraps, folders);
    }
}
