package com.campusnavi.backend.feed.service;

import com.campusnavi.backend.feed.dto.CardListResponse;
import com.campusnavi.backend.feed.dto.DeadlineListResponse;
import com.campusnavi.backend.global.common.AuthContext;
import com.campusnavi.backend.global.exception.BusinessException;
import com.campusnavi.backend.global.exception.ErrorCode;
import com.campusnavi.backend.member.entity.Member;
import com.campusnavi.backend.member.entity.MemberRole;
import com.campusnavi.backend.member.repository.MemberRepository;
import com.campusnavi.backend.official.post.dto.OfficialPostCardResponse;
import com.campusnavi.backend.official.post.recommend.service.RecommendQueryService;
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
    private final RecommendQueryService recommendQueryService;
    private final MemberRepository memberRepository;

    public CardListResponse getCardLists(AuthContext context) {
        Member requester = memberRepository.findById(context.memberId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        List<OfficialPostCardResponse> recent = officialPostQueryService.getRecentPosts(context);
        List<OfficialPostCardResponse> recommend = requester.getRole() == MemberRole.ADMIN
                ? recent
                : recommendQueryService.getRecommendPosts(context, requester);

        return new CardListResponse(recent, recommend);
    }

    public DeadlineListResponse getDeadlinePostsForFeed(AuthContext context) {
        return new DeadlineListResponse(officialPostQueryService.getDeadlinePostsForFeed(context));
    }

    public DeadlineListResponse getAllDeadlinePosts(AuthContext context) {
        return new DeadlineListResponse(officialPostQueryService.getAllDeadlinePosts(context));
    }
}
