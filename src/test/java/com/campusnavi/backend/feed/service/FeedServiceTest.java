package com.campusnavi.backend.feed.service;

import com.campusnavi.backend.feed.dto.CardListResponse;
import com.campusnavi.backend.global.common.AuthContext;
import com.campusnavi.backend.global.exception.BusinessException;
import com.campusnavi.backend.global.exception.ErrorCode;
import com.campusnavi.backend.member.entity.Member;
import com.campusnavi.backend.member.entity.MemberRole;
import com.campusnavi.backend.member.repository.MemberRepository;
import com.campusnavi.backend.official.post.dto.OfficialPostCardResponse;
import com.campusnavi.backend.official.post.recommend.service.RecommendQueryService;
import com.campusnavi.backend.official.post.service.OfficialPostQueryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class FeedServiceTest {

    @Mock
    private OfficialPostQueryService officialPostQueryService;

    @Mock
    private RecommendQueryService recommendQueryService;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private FeedService feedService;

    private static final Long MEMBER_ID = 1L;
    private static final Long UNIVERSITY_ID = 100L;
    private static final AuthContext CONTEXT = new AuthContext(MEMBER_ID, UNIVERSITY_ID);

    @Nested
    @DisplayName("getCardLists")
    class GetCardLists {

        @Test
        @DisplayName("USER는 recent와 recommend를 각각 받아 응답한다")
        void user() {
            // given
            Member requester = activeMember(MemberRole.USER);
            given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(requester));

            List<OfficialPostCardResponse> recent = List.of(response(1L));
            List<OfficialPostCardResponse> recommend = List.of(response(7L));
            given(officialPostQueryService.getRecentPosts(CONTEXT)).willReturn(recent);
            given(recommendQueryService.getRecommendPosts(eq(CONTEXT), any(Member.class)))
                    .willReturn(recommend);

            // when
            CardListResponse result = feedService.getCardLists(CONTEXT);

            // then
            assertThat(result.newPosts()).extracting(OfficialPostCardResponse::postId).containsExactly(1L);
            assertThat(result.recommendedPosts()).extracting(OfficialPostCardResponse::postId).containsExactly(7L);
        }

        @Test
        @DisplayName("ADMIN은 recent를 recommend 자리에도 그대로 재사용한다")
        void admin() {
            // given
            Member admin = activeMember(MemberRole.ADMIN);
            given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(admin));

            List<OfficialPostCardResponse> recent = List.of(response(1L), response(2L));
            given(officialPostQueryService.getRecentPosts(CONTEXT)).willReturn(recent);

            // when
            CardListResponse result = feedService.getCardLists(CONTEXT);

            // then
            assertThat(result.newPosts()).isSameAs(recent);
            assertThat(result.recommendedPosts()).isSameAs(recent);
            then(recommendQueryService).should(never()).getRecommendPosts(any(), any());
        }

        @Test
        @DisplayName("회원을 찾지 못하면 MEMBER_NOT_FOUND 예외가 발생한다")
        void memberNotFound() {
            // given
            given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> feedService.getCardLists(CONTEXT))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);

            then(officialPostQueryService).shouldHaveNoInteractions();
            then(recommendQueryService).shouldHaveNoInteractions();
        }
    }

    private static Member activeMember(MemberRole role) {
        Member member = Member.join(
                "u@test.ac.kr", "user", "password", "nick",
                UNIVERSITY_ID, null, 2022, 3);
        if (role != MemberRole.USER) {
            ReflectionTestUtils.setField(member, "role", role);
        }
        return member;
    }

    private static OfficialPostCardResponse response(long postId) {
        return new OfficialPostCardResponse(
                postId, "title-" + postId, "tag-" + postId, "summary-" + postId,
                null, LocalDate.of(2026, 5, 1));
    }
}
