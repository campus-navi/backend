package com.campusnavi.backend.official.post.recommend.service;

import com.campusnavi.backend.member.dto.MemberScope;
import com.campusnavi.backend.member.entity.Member;
import com.campusnavi.backend.member.repository.MemberInterestRepository;
import com.campusnavi.backend.member.repository.MemberQueryRepository;
import com.campusnavi.backend.official.post.dto.OfficialPostRecommendCandidateRaw;
import com.campusnavi.backend.official.post.dto.OfficialPostScopeCondition;
import com.campusnavi.backend.official.post.dto.OfficialPostStatsRaw;
import com.campusnavi.backend.official.post.recommend.repository.FeedRecommendSnapshotRepository;
import com.campusnavi.backend.official.post.recommend.repository.RecommendQueryRepository;
import com.campusnavi.backend.official.post.recommend.util.RecommendSlot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


@Component
@RequiredArgsConstructor
public class RecommendSnapshotBuilder {

    private final MemberQueryRepository memberQueryRepository;
    private final MemberInterestRepository interestRepository;
    private final RecommendationScoringService scoringService;
    private final RecommendQueryRepository recommendQueryRepository;
    private final FeedRecommendSnapshotRepository snapshotRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<Long> computeAndUpsert(Member requester) {
        Long memberId = requester.getId();
        OfficialPostScopeCondition condition = toCondition(requester);
        LocalDateTime slotAt = RecommendSlot.current(LocalDateTime.now());

        List<Long> ranked = computeRankedIds(memberId, requester, condition, slotAt);

        if (ranked.isEmpty()) {
            return ranked;
        }

        snapshotRepository.upsert(memberId, toJsonArray(ranked));
        return ranked;
    }

    private List<Long> computeRankedIds(
            Long memberId, Member requester,
            OfficialPostScopeCondition condition, LocalDateTime slotAt) {

        List<OfficialPostRecommendCandidateRaw> candidates =
                recommendQueryRepository.findRecommendCandidates(condition, slotAt, memberId);

        if (candidates.isEmpty()) return List.of();

        List<Long> candidateIds = candidates.stream()
                .map(OfficialPostRecommendCandidateRaw::postId).toList();

        Map<Long, OfficialPostStatsRaw> statsMap =
                recommendQueryRepository.findStatsByPostIds(
                                candidateIds, requester.getAdmissionYear(), requester.getGrade(),
                                condition.departmentIds())
                        .stream()
                        .collect(Collectors.toMap(OfficialPostStatsRaw::postId, s -> s));

        Set<Long> interestTagIds =
                Set.copyOf(interestRepository.findTagIdsByMemberId(memberId));

        long deptCount = memberQueryRepository
                .countActiveMembersInDepartments(condition.departmentIds());

        return scoringService.rank(candidates, statsMap, interestTagIds, deptCount).stream()
                .map(OfficialPostRecommendCandidateRaw::postId).toList();
    }

    private OfficialPostScopeCondition toCondition(Member requester) {
        List<MemberScope> memberScopes = memberQueryRepository.findScopesByMemberId(requester.getId());
        return OfficialPostScopeCondition.from(requester.getUniversityId(), memberScopes);
    }


    private static String toJsonArray(List<Long> ids) {
        return ids.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(",", "[", "]"));
    }
}
