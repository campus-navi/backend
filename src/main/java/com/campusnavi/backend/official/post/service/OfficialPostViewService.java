package com.campusnavi.backend.official.post.service;

import com.campusnavi.backend.official.post.repository.OfficialPostViewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OfficialPostViewService {

    private final OfficialPostViewRepository viewRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordView(Long memberId, Long postId) {
        viewRepository.upsert(memberId, postId);
    }
}
