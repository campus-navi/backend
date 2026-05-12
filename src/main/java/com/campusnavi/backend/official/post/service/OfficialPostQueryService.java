package com.campusnavi.backend.official.post.service;

import com.campusnavi.backend.global.common.AuthContext;
import com.campusnavi.backend.member.dto.MemberScope;
import com.campusnavi.backend.member.repository.MemberQueryRepository;
import com.campusnavi.backend.official.post.dto.DeadlinePostRaw;
import com.campusnavi.backend.official.post.dto.DeadlinePostResponse;
import com.campusnavi.backend.official.post.dto.OfficialPostCardRaw;
import com.campusnavi.backend.official.post.dto.OfficialPostCardResponse;
import com.campusnavi.backend.official.post.dto.OfficialPostScopeCondition;
import com.campusnavi.backend.official.post.recommend.util.RecommendSlot;
import com.campusnavi.backend.official.post.repository.OfficialPostQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OfficialPostQueryService {

    private final MemberQueryRepository memberQueryRepository;
    private final OfficialPostQueryRepository officialPostQueryRepository;
    private final OfficialPostCardResponseMapper cardResponseMapper;

    public List<OfficialPostCardResponse> getRecentPosts(AuthContext context) {
        OfficialPostScopeCondition condition = toCondition(context);
        LocalDateTime slotAt = RecommendSlot.current(LocalDateTime.now());

        List<OfficialPostCardRaw> rawList = officialPostQueryRepository.findRecentPosts(condition, slotAt);

        return rawList.stream()
                .map(cardResponseMapper::toResponse)
                .toList();
    }

    public List<DeadlinePostResponse> getDeadlinePostsForFeed(AuthContext context) {
        OfficialPostScopeCondition condition = toCondition(context);
        return officialPostQueryRepository.findDeadlinePostsForFeed(condition, context.memberId())
                .stream()
                .map(this::toDeadlineResponse)
                .toList();
    }

    public List<DeadlinePostResponse> getAllDeadlinePosts(AuthContext context) {
        OfficialPostScopeCondition condition = toCondition(context);
        return officialPostQueryRepository.findAllDeadlinePosts(condition, context.memberId())
                .stream()
                .map(this::toDeadlineResponse)
                .toList();
    }

    private OfficialPostScopeCondition toCondition(AuthContext context) {
        List<MemberScope> memberScopes = memberQueryRepository.findScopesByMemberId(context.memberId());
        return OfficialPostScopeCondition.from(context.universityId(), memberScopes);
    }

    private DeadlinePostResponse toDeadlineResponse(DeadlinePostRaw raw) {
        return new DeadlinePostResponse(
                raw.postId(),
                raw.title(),
                raw.tagName(),
                raw.publisher(),
                raw.publishedAt(),
                raw.endDate(),
                raw.isNotificationOn()
        );
    }
}
