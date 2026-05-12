package com.campusnavi.backend.official.post.service;

import com.campusnavi.backend.global.common.AuthContext;
import com.campusnavi.backend.global.exception.BusinessException;
import com.campusnavi.backend.global.exception.ErrorCode;
import com.campusnavi.backend.infra.storage.S3StorageService;
import com.campusnavi.backend.member.dto.MemberScope;
import com.campusnavi.backend.member.entity.Member;
import com.campusnavi.backend.member.entity.MemberRole;
import com.campusnavi.backend.member.repository.MemberInterestRepository;
import com.campusnavi.backend.member.repository.MemberQueryRepository;
import com.campusnavi.backend.member.repository.MemberRepository;
import com.campusnavi.backend.official.post.dto.*;
import com.campusnavi.backend.official.post.recommend.service.RecommendationScoringService;
import com.campusnavi.backend.official.post.repository.OfficialPostQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OfficialPostQueryService {

    private final MemberQueryRepository memberQueryRepository;
    private final MemberRepository memberRepository;
    private final OfficialPostQueryRepository officialPostQueryRepository;
    private final S3StorageService s3StorageService;
    private final MemberInterestRepository interestRepository;
    private final RecommendationScoringService scoringService;

    public List<OfficialPostCardResponse> getRecentPosts(AuthContext context) {
        OfficialPostScopeCondition condition = toCondition(context);

        List<OfficialPostCardRaw> rawList = officialPostQueryRepository.findRecentPosts(condition);

        return rawList.stream()
                .map(this::toResponse)
                .toList();
    }

    public List<OfficialPostCardResponse> getRecommendPosts(AuthContext context) {
        Member requester = memberRepository.findById(context.memberId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        if (requester.getRole() == MemberRole.ADMIN) {
            return getRecentPosts(context);
        }

        OfficialPostScopeCondition condition = toCondition(context);

        List<OfficialPostRecommendCandidateRaw> candidates =
                officialPostQueryRepository.findRecommendCandidates(condition);

        if (candidates.isEmpty()) return List.of();

        List<Long> postIds = candidates.stream()
                .map(OfficialPostRecommendCandidateRaw::postId).toList();

        Map<Long, OfficialPostStatsRaw> statsMap =
                officialPostQueryRepository.findStatsByPostIds(
                                postIds, requester.getAdmissionYear(), requester.getGrade(),
                                condition.departmentIds())
                        .stream()
                        .collect(Collectors.toMap(OfficialPostStatsRaw::postId, s -> s));

        Set<Long> interestTagIds =
                Set.copyOf(interestRepository.findTagIdsByMemberId(context.memberId()));

        long deptCount = memberQueryRepository
                .countActiveMembersInDepartments(condition.departmentIds());

        List<OfficialPostRecommendCandidateRaw> ranked = scoringService.rank(
                candidates, statsMap, interestTagIds, deptCount);

        return ranked.stream().map(this::toRecommendResponse).toList();
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

    private OfficialPostCardResponse toRecommendResponse(OfficialPostRecommendCandidateRaw raw) {
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
