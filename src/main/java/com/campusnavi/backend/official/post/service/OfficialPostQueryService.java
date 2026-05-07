package com.campusnavi.backend.official.post.service;

import com.campusnavi.backend.global.common.AuthContext;
import com.campusnavi.backend.infra.storage.S3StorageService;
import com.campusnavi.backend.member.dto.MemberScope;
import com.campusnavi.backend.member.repository.MemberInterestRepository;
import com.campusnavi.backend.member.repository.MemberQueryRepository;
import com.campusnavi.backend.official.post.dto.DeadlinePostRaw;
import com.campusnavi.backend.official.post.dto.DeadlinePostResponse;
import com.campusnavi.backend.official.post.dto.OfficialPostCardRaw;
import com.campusnavi.backend.official.post.dto.OfficialPostCardResponse;
import com.campusnavi.backend.official.post.dto.OfficialPostScopeCondition;
import com.campusnavi.backend.official.post.repository.OfficialPostQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OfficialPostQueryService {

    private final MemberQueryRepository memberQueryRepository;
    private final OfficialPostQueryRepository officialPostQueryRepository;
    private final S3StorageService s3StorageService;
    private final MemberInterestRepository interestRepository;

    public List<OfficialPostCardResponse> getRecentPosts(AuthContext context) {
        OfficialPostScopeCondition condition = toCondition(context);

        List<OfficialPostCardRaw> rawList = officialPostQueryRepository.findRecentPosts(condition);

        return rawList.stream()
                .map(this::toResponse)
                .toList();
    }

    public List<OfficialPostCardResponse> getRecommendPosts(AuthContext context) {
        OfficialPostScopeCondition condition = toCondition(context);

        List<Long> tagIds = interestRepository.findTagIdsByMemberId(context.memberId());

        List<OfficialPostCardRaw> rawList = officialPostQueryRepository.findRecommendPosts(condition, tagIds);

        return rawList.stream()
                .map(this::toResponse)
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

    private OfficialPostCardResponse toResponse(OfficialPostCardRaw raw) {
        return new OfficialPostCardResponse(
                raw.postId(),
                raw.title(),
                raw.tagName(),
                raw.summary(),
                resolveImageUrl(raw.s3Key()),
                raw.publishedAt()
        );
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

    private String resolveImageUrl(String s3Key) {
        if (s3Key == null || s3Key.isBlank()) return null;
        return s3StorageService.resolveUrl(s3Key);
    }
}
