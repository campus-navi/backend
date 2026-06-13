package com.campusnavi.backend.tag.service;

import com.campusnavi.backend.tag.dto.TagResponse;
import com.campusnavi.backend.tag.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;

    @Transactional(readOnly = true)
    public List<TagResponse> getTags() {
        return tagRepository.findByIsRecommendableTrueOrderByIdAsc()
                .stream()
                .map(TagResponse::of)
                .toList();
    }

}
