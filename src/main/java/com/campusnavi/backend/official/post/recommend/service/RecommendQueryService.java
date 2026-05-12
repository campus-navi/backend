package com.campusnavi.backend.official.post.recommend.service;

import com.campusnavi.backend.global.common.AuthContext;
import com.campusnavi.backend.member.dto.MemberScope;
import com.campusnavi.backend.member.entity.Member;
import com.campusnavi.backend.member.repository.MemberInterestRepository;
import com.campusnavi.backend.member.repository.MemberQueryRepository;
import com.campusnavi.backend.official.post.dto.OfficialPostCardRaw;
import com.campusnavi.backend.official.post.dto.OfficialPostCardResponse;
import com.campusnavi.backend.official.post.dto.OfficialPostRecommendCandidateRaw;
import com.campusnavi.backend.official.post.dto.OfficialPostScopeCondition;
import com.campusnavi.backend.official.post.dto.OfficialPostStatsRaw;
import com.campusnavi.backend.official.post.recommend.entity.FeedRecommendSnapshot;
import com.campusnavi.backend.official.post.recommend.repository.FeedRecommendSnapshotRepository;
import com.campusnavi.backend.official.post.recommend.repository.RecommendQueryRepository;
import com.campusnavi.backend.official.post.recommend.util.RecommendSlot;
import com.campusnavi.backend.official.post.service.OfficialPostCardResponseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendQueryService {

    private final MemberQueryRepository memberQueryRepository;
    private final MemberInterestRepository interestRepository;
    private final RecommendationScoringService scoringService;
    private final RecommendQueryRepository recommendQueryRepository;
    private final FeedRecommendSnapshotRepository snapshotRepository;
    private final RecommendSnapshotWriter snapshotWriter;
    private final OfficialPostCardResponseMapper cardResponseMapper;

    /**
     * 호출자(FeedService)가 USER 임을 보장. ADMIN 분기는 FeedService에서 처리.
     */
    public List<OfficialPostCardResponse> getRecommendPosts(AuthContext context, Member requester) {
        OfficialPostScopeCondition condition = toCondition(context);
        LocalDateTime slotAt = RecommendSlot.current(LocalDateTime.now());

        List<Long> postIds = snapshotRepository
                .findByMemberIdAndSlotAt(context.memberId(), slotAt)
                .map(FeedRecommendSnapshot::getPostIds)
                .orElseGet(() -> persistOrLoadExisting(
                        context.memberId(), slotAt,
                        computeRankedIds(context, requester, condition, slotAt)));

        if (postIds.isEmpty()) return List.of();

        Map<Long, OfficialPostCardRaw> byId = recommendQueryRepository.findCardsByIds(postIds)
                .stream()
                .collect(Collectors.toMap(OfficialPostCardRaw::postId, c -> c));

        return postIds.stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .map(cardResponseMapper::toResponse)
                .toList();
    }

    /**
     * Writer는 별도 REQUIRES_NEW 트랜잭션으로 INSERT 만 시도한다.
     * UNIQUE 충돌이면 그 내부 트랜잭션만 abort/rollback 되고
     * 외부 readOnly 트랜잭션(이 메서드가 속한 tx)은 살아 있으므로 후속 SELECT 가 안전하다.
     */
    private List<Long> persistOrLoadExisting(Long memberId, LocalDateTime slotAt, List<Long> ids) {
        try {
            snapshotWriter.persist(memberId, slotAt, ids);
            return ids;
        } catch (DataIntegrityViolationException e) {
            return snapshotRepository.findByMemberIdAndSlotAt(memberId, slotAt)
                    .map(FeedRecommendSnapshot::getPostIds)
                    .orElseThrow(() -> e);
        }
    }

    private List<Long> computeRankedIds(
            AuthContext context, Member requester,
            OfficialPostScopeCondition condition, LocalDateTime slotAt) {

        List<OfficialPostRecommendCandidateRaw> candidates =
                recommendQueryRepository.findRecommendCandidates(condition, slotAt, context.memberId());

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
                Set.copyOf(interestRepository.findTagIdsByMemberId(context.memberId()));

        long deptCount = memberQueryRepository
                .countActiveMembersInDepartments(condition.departmentIds());

        return scoringService.rank(candidates, statsMap, interestTagIds, deptCount).stream()
                .map(OfficialPostRecommendCandidateRaw::postId).toList();
    }

    private OfficialPostScopeCondition toCondition(AuthContext context) {
        List<MemberScope> memberScopes = memberQueryRepository.findScopesByMemberId(context.memberId());
        return OfficialPostScopeCondition.from(context.universityId(), memberScopes);
    }
}
