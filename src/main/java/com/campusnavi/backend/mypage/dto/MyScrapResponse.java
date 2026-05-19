package com.campusnavi.backend.mypage.dto;

import com.campusnavi.backend.official.post.dto.RecentScrapResponse;
import com.campusnavi.backend.scrap.dto.ScrapFolderResponse;

import java.util.List;

public record MyScrapResponse(
        List<RecentScrapResponse> recentScraps,
        List<ScrapFolderResponse> folders
) {
}
