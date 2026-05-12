package com.campusnavi.backend.official.post.recommend.service;

import com.campusnavi.backend.official.post.recommend.entity.FeedRecommendSnapshot;
import com.campusnavi.backend.official.post.recommend.repository.FeedRecommendSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RecommendSnapshotWriter {

    private final FeedRecommendSnapshotRepository snapshotRepository;

    /**
     * 추천 스냅샷을 별도 트랜잭션(REQUIRES_NEW)에서 INSERT 한다.
     * UNIQUE 충돌 시 {@link org.springframework.dao.DataIntegrityViolationException}이
     * 호출자(외부 트랜잭션)로 전파되며, 격리된 내부 트랜잭션만 abort/rollback 된다.
     * 외부 트랜잭션의 connection은 그대로 살아 있어 후속 SELECT(재조회)가 안전하다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persist(Long memberId, LocalDateTime slotAt, List<Long> ids) {
        snapshotRepository.save(FeedRecommendSnapshot.create(memberId, slotAt, ids));
    }
}
