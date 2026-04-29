package com.campusnavi.backend.feed.service;

import com.campusnavi.backend.feed.dto.CardListResponse;
import com.campusnavi.backend.feed.dto.DeadlineListResponse;
import com.campusnavi.backend.global.common.AuthContext;
import com.campusnavi.backend.official.post.dto.OfficialPostCardResponse;
import com.campusnavi.backend.official.post.service.OfficialPostQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedService {

    private final OfficialPostQueryService officialPostQueryService;

    public CardListResponse getCardLists(AuthContext context) {
        List<OfficialPostCardResponse> recentPosts = officialPostQueryService.getRecentPosts(context);
        List<OfficialPostCardResponse> recommendPosts = officialPostQueryService.getRecommendPosts(context);
        return new CardListResponse(recentPosts, recommendPosts);
    }

    public DeadlineListResponse getDeadlinePostsForFeed(AuthContext context) {
        return new DeadlineListResponse(officialPostQueryService.getDeadlinePostsForFeed(context));
    }

    public DeadlineListResponse getAllDeadlinePosts(AuthContext context) {
        return new DeadlineListResponse(officialPostQueryService.getAllDeadlinePosts(context));
    }
}
