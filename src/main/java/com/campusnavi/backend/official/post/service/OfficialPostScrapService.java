package com.campusnavi.backend.official.post.service;

import com.campusnavi.backend.global.common.AuthContext;
import com.campusnavi.backend.global.exception.BusinessException;
import com.campusnavi.backend.global.exception.ErrorCode;
import com.campusnavi.backend.official.post.entity.OfficialPost;
import com.campusnavi.backend.official.post.entity.OfficialPostScrap;
import com.campusnavi.backend.official.post.repository.OfficialPostRepository;
import com.campusnavi.backend.official.post.repository.OfficialPostScrapRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class OfficialPostScrapService {

    private final OfficialPostRepository postRepository;
    private final OfficialPostScrapRepository scrapRepository;

    public void scrap(Long postId, AuthContext context) {
        OfficialPost post = postRepository.findActiveByIdAndUniversityScope(postId, context.universityId())
                .orElseThrow(() -> new BusinessException(ErrorCode.OFFICIAL_POST_NOT_FOUND));

        if (scrapRepository.existsByMemberIdAndPostId(context.memberId(), postId)) {
            return;
        }

        scrapRepository.save(OfficialPostScrap.create(context.memberId(), post));
    }

    public void unscrap(Long postId, AuthContext context) {
        if (!postRepository.existsActiveByIdAndUniversityScope(postId, context.universityId())) {
            throw new BusinessException(ErrorCode.OFFICIAL_POST_NOT_FOUND);
        }

        scrapRepository.findByMemberIdAndPostId(context.memberId(), postId)
                .ifPresent(scrapRepository::delete);
    }
}
