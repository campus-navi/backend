package com.campusnavi.backend.official.post.service;

import com.campusnavi.backend.global.common.AuthContext;
import com.campusnavi.backend.member.repository.MemberQueryRepository;
import com.campusnavi.backend.official.post.repository.OfficialPostQueryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class OfficialPostQueryServiceTest {

    @Mock
    private MemberQueryRepository memberQueryRepository;

    @Mock
    private OfficialPostQueryRepository officialPostQueryRepository;

    @Mock
    private OfficialPostCardResponseMapper cardResponseMapper;

    @InjectMocks
    private OfficialPostQueryService officialPostQueryService;

    private static final Long MEMBER_ID = 1L;
    private static final Long UNIVERSITY_ID = 100L;
    private static final AuthContext CONTEXT = new AuthContext(MEMBER_ID, UNIVERSITY_ID);

    @Nested
    @DisplayName("getRecentPosts")
    class GetRecentPosts {

        @Test
        @DisplayName("현재 슬롯이 쿼리에 전달된다")
        void slotAtPropagation() {
            // given
            given(memberQueryRepository.findScopesByMemberId(MEMBER_ID)).willReturn(List.of());
            given(officialPostQueryRepository.findRecentPosts(any(), any())).willReturn(List.of());

            // when
            officialPostQueryService.getRecentPosts(CONTEXT);

            // then
            ArgumentCaptor<LocalDateTime> slotCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
            then(officialPostQueryRepository).should().findRecentPosts(any(), slotCaptor.capture());

            LocalDateTime slot = slotCaptor.getValue();
            assertThat(slot.getMinute()).isZero();
            assertThat(slot.getHour()).isIn(9, 18);
        }
    }
}
