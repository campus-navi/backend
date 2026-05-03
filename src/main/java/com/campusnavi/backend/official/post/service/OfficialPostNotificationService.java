package com.campusnavi.backend.official.post.service;

import com.campusnavi.backend.global.common.AuthContext;
import com.campusnavi.backend.global.exception.BusinessException;
import com.campusnavi.backend.global.exception.ErrorCode;
import com.campusnavi.backend.official.post.entity.OfficialPost;
import com.campusnavi.backend.official.post.entity.OfficialPostNotification;
import com.campusnavi.backend.official.post.repository.OfficialPostNotificationRepository;
import com.campusnavi.backend.official.post.repository.OfficialPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class OfficialPostNotificationService {

    private final OfficialPostRepository postRepository;
    private final OfficialPostNotificationRepository notificationRepository;

    public void enable(Long postId, AuthContext context) {
        OfficialPost post = postRepository.findActiveByIdAndUniversityScope(postId, context.universityId())
                .orElseThrow(() -> new BusinessException(ErrorCode.OFFICIAL_POST_NOT_FOUND));

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
}
