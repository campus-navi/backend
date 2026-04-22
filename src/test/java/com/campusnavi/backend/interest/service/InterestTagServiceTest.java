package com.campusnavi.backend.interest.service;

import com.campusnavi.backend.interest.dto.InterestTagResponse;
import com.campusnavi.backend.interest.entity.InterestTag;
import com.campusnavi.backend.interest.repository.InterestTagRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InterestTagServiceTest {

    @Mock
    private InterestTagRepository interestTagRepository;

    @InjectMocks
    private InterestTagService interestTagService;

    @Test
    @DisplayName("is_recommendable=true인 태그를 sort_order 오름차순으로 반환한다")
    void getInterestTags_returnsSortedRecommendableTags() {
        // given
        InterestTag tag1 = mock(InterestTag.class);
        InterestTag tag2 = mock(InterestTag.class);
        when(tag1.getId()).thenReturn(1L);
        when(tag1.getName()).thenReturn("장학금");
        when(tag2.getId()).thenReturn(2L);
        when(tag2.getName()).thenReturn("취업·채용");
        given(interestTagRepository.findByIsRecommendableTrueOrderBySortOrderAsc()).willReturn(List.of(tag1, tag2));

        // when
        List<InterestTagResponse> result = interestTagService.getInterestTags();

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).name()).isEqualTo("장학금");
        assertThat(result.get(1).name()).isEqualTo("취업·채용");
    }

    @Test
    @DisplayName("추천 가능한 태그가 없으면 빈 목록을 반환한다")
    void getInterestTags_returnsEmptyList() {
        // given
        given(interestTagRepository.findByIsRecommendableTrueOrderBySortOrderAsc()).willReturn(List.of());

        // when
        List<InterestTagResponse> result = interestTagService.getInterestTags();

        // then
        assertThat(result).isEmpty();
    }
}
