package com.campusnavi.backend.official.ai.service;

import com.campusnavi.backend.official.ai.dto.OfficialAiResponse;
import com.campusnavi.backend.official.post.entity.ApplyMethodType;
import com.campusnavi.backend.official.post.entity.OfficialPostAiMeta;
import com.campusnavi.backend.official.post.repository.OfficialPostAiMetaRepository;
import com.campusnavi.backend.tag.entity.Tag;
import com.campusnavi.backend.tag.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AiMetaService {

    private final OfficialPostAiMetaRepository metaRepository;
    private final TagRepository tagRepository;

    @Transactional
    public void saveResult(Long postId, OfficialAiResponse response) {
        OfficialPostAiMeta meta = metaRepository.findByOfficialPostId(postId).orElseThrow();

        Tag tag = tagRepository.findByCode(response.tagCode()).orElse(null);

        ApplyMethodType applyMethodType = response.applyMethodType() != null
                ? ApplyMethodType.valueOf(response.applyMethodType())
                : null;

        meta.processCompleted(
                response.summary(),
                response.targetGradeMin(),
                response.targetGradeMax(),
                tag, response.keywords(),
                response.contactPhone(), response.contactEmail(),
                response.startDate(), response.startTime(),
                response.endDate(), response.endTime(),
                response.requiredDocuments(),
                applyMethodType, response.applyMethodDetail(),
                response.eligibility(),
                response.isApplicable()
        );
    }

    @Transactional
    public void markFailed(Long postId, String reason) {
        metaRepository.findByOfficialPostId(postId)
                .ifPresent(meta -> meta.processFailed(reason));
    }
}
