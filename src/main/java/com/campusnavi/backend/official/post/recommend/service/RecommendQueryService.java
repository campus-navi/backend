package com.campusnavi.backend.official.post.recommend.service;

import com.campusnavi.backend.global.common.AuthContext;
import com.campusnavi.backend.member.entity.Member;
import com.campusnavi.backend.official.post.dto.OfficialPostCardRaw;
import com.campusnavi.backend.official.post.dto.OfficialPostCardResponse;
import com.campusnavi.backend.official.post.recommend.entity.FeedRecommendSnapshot;
import com.campusnavi.backend.official.post.recommend.repository.FeedRecommendSnapshotRepository;
import com.campusnavi.backend.official.post.recommend.repository.RecommendQueryRepository;
import com.campusnavi.backend.official.post.service.OfficialPostCardResponseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendQueryService {

    private final FeedRecommendSnapshotRepository snapshotRepository;
    private final RecommendQueryRepository recommendQueryRepository;
    private final RecommendSnapshotBuilder snapshotBuilder;
    private final OfficialPostCardResponseMapper cardResponseMapper;


    public List<OfficialPostCardResponse> getRecommendPosts(AuthContext context, Member requester) {
        Long memberId = context.memberId();

        List<Long> postIds = snapshotRepository.findByMemberId(memberId)
                .map(FeedRecommendSnapshot::getPostIds)
                .filter(ids -> !ids.isEmpty())
                .orElseGet(() -> snapshotBuilder.computeAndUpsert(requester));

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
}
