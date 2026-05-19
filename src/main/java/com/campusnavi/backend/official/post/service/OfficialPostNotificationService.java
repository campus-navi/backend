package com.campusnavi.backend.official.post.service;

import com.campusnavi.backend.global.common.AuthContext;
import com.campusnavi.backend.global.exception.BusinessException;
import com.campusnavi.backend.global.exception.ErrorCode;
import com.campusnavi.backend.official.post.entity.OfficialPost;
import com.campusnavi.backend.official.post.entity.OfficialPostAiMeta;
import com.campusnavi.backend.official.post.dto.RemindBulkDeleteResponse;
import com.campusnavi.backend.official.post.entity.OfficialPostNotification;
import com.campusnavi.backend.official.post.repository.OfficialPostAiMetaRepository;
import com.campusnavi.backend.official.post.repository.OfficialPostNotificationRepository;
import com.campusnavi.backend.official.post.repository.OfficialPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class OfficialPostNotificationService {

    private final OfficialPostRepository postRepository;
    private final OfficialPostNotificationRepository notificationRepository;
    private final OfficialPostAiMetaRepository aiMetaRepository;

    public void enable(Long postId, AuthContext context) {
        OfficialPost post = postRepository.findActiveByIdAndUniversityScope(postId, context.universityId())
                .orElseThrow(() -> new BusinessException(ErrorCode.OFFICIAL_POST_NOT_FOUND));

        LocalDate endDate = aiMetaRepository.findByOfficialPostId(postId)
                .map(OfficialPostAiMeta::getEndDate)
                .orElse(null);
        if (endDate == null || endDate.isBefore(LocalDate.now())) {
            throw new BusinessException(ErrorCode.OFFICIAL_POST_DEADLINE_REQUIRED);
        }

        if (notificationRepository.existsByMemberIdAndPostId(context.memberId(), postId)) {
            return;
        }

        notificationRepository.save(OfficialPostNotification.create(context.memberId(), post));
    }

    public void disable(Long postId, AuthContext context) {
        if (!postRepository.existsActiveByIdAndUniversityScope(postId, context.universityId())) {
            throw new BusinessException(ErrorCode.OFFICIAL_POST_NOT_FOUND);
        }

        notificationRepository.findByMemberIdAndPostId(context.memberId(), postId)
                .ifPresent(notificationRepository::delete);
    }

    public RemindBulkDeleteResponse deleteReminds(List<Long> postIds, AuthContext context) {
        Set<Long> ids = Set.copyOf(postIds);
        List<Long> deletedPostIds = notificationRepository.findExistingPostIds(context.memberId(), ids);
        if (deletedPostIds.isEmpty()) {
            return new RemindBulkDeleteResponse(0, List.of());
        }

        int deletedCount = notificationRepository.deleteByMemberIdAndPostIdIn(context.memberId(), ids);
        return new RemindBulkDeleteResponse(deletedCount, deletedPostIds);
    }

    // 복구는 직전 상태 복원이므로 enable의 마감일 가드를 적용하지 않음 — 만료 시 조회 필터로 비렌더링
    public void restoreReminds(List<Long> postIds, AuthContext context) {
        Set<Long> targetPostIds = Set.copyOf(postIds);
        Set<Long> already = Set.copyOf(
                notificationRepository.findExistingPostIds(context.memberId(), targetPostIds));

        List<OfficialPostNotification> toSave = postRepository
                .findByIdInAndUniversityScope(targetPostIds, context.universityId()).stream()
                .filter(post -> !already.contains(post.getId()))
                .map(post -> OfficialPostNotification.create(context.memberId(), post))
                .toList();
        if (toSave.isEmpty()) {
            return;
        }
        notificationRepository.saveAll(toSave);
    }
}
