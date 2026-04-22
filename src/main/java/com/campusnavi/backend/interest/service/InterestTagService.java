package com.campusnavi.backend.interest.service;

import com.campusnavi.backend.interest.dto.InterestTagResponse;
import com.campusnavi.backend.interest.repository.InterestTagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InterestTagService {

    private final InterestTagRepository interestTagRepository;

    @Transactional(readOnly = true)
    public List<InterestTagResponse> getInterestTags() {
        return interestTagRepository.findByIsRecommendableTrueOrderBySortOrderAsc()
                .stream()
                .map(InterestTagResponse::of)
                .toList();
    }
}
