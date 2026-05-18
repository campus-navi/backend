package com.campusnavi.backend.mypage.service;

import com.campusnavi.backend.community.post.service.PostInteractionService;
import com.campusnavi.backend.member.dto.MemberProfile;
import com.campusnavi.backend.member.service.MemberService;
import com.campusnavi.backend.mypage.dto.MyPageResponse;
import com.campusnavi.backend.notification.service.RemindNotificationService;
import com.campusnavi.backend.official.post.service.OfficialPostScrapService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class MyPageServiceTest {

    @Mock
    private MemberService memberService;

    @Mock
    private OfficialPostScrapService officialPostScrapService;

    @Mock
    private PostInteractionService postInteractionService;

    @Mock
    private RemindNotificationService remindNotificationService;

    @InjectMocks
    private MyPageService service;

    private static final Long MEMBER_ID = 7L;

    @Nested
    @DisplayName("마이페이지 조회")
    class GetMyPage {

        @Test
        @DisplayName("프로필과 스크랩 합계, 활성 리마인드 수를 조립해 반환한다")
        void success() {
            MemberProfile profile = new MemberProfile("testnick", "user@test.ac.kr",
                    "테스트대학교(서울캠퍼스)", 25, 1, List.of("컴퓨터공학과"));
            given(memberService.getMyProfile(MEMBER_ID)).willReturn(profile);
            given(officialPostScrapService.countScrappedPosts(MEMBER_ID)).willReturn(3L);
            given(postInteractionService.countScraps(MEMBER_ID)).willReturn(2L);
            given(remindNotificationService.getActiveRemindCount(MEMBER_ID)).willReturn(4L);
            given(memberService.countMyInterests(MEMBER_ID)).willReturn(3L);

            MyPageResponse result = service.getMyPage(MEMBER_ID);

            assertThat(result.nickname()).isEqualTo("testnick");
            assertThat(result.email()).isEqualTo("user@test.ac.kr");
            assertThat(result.campus()).isEqualTo("테스트대학교(서울캠퍼스)");
            assertThat(result.admissionYear()).isEqualTo(25);
            assertThat(result.grade()).isEqualTo(1);
            assertThat(result.departments()).containsExactly("컴퓨터공학과");
            assertThat(result.scrapCount()).isEqualTo(5L);
            assertThat(result.remindCount()).isEqualTo(4L);
            assertThat(result.interestCount()).isEqualTo(3L);
        }

        @Test
        @DisplayName("스크랩과 리마인드가 없으면 0을 반환한다")
        void zeroCounts() {
            MemberProfile profile = new MemberProfile("testnick", "user@test.ac.kr",
                    "테스트대학교(서울캠퍼스)", 25, 1, List.of());
            given(memberService.getMyProfile(MEMBER_ID)).willReturn(profile);
            given(officialPostScrapService.countScrappedPosts(MEMBER_ID)).willReturn(0L);
            given(postInteractionService.countScraps(MEMBER_ID)).willReturn(0L);
            given(remindNotificationService.getActiveRemindCount(MEMBER_ID)).willReturn(0L);
            given(memberService.countMyInterests(MEMBER_ID)).willReturn(0L);

            MyPageResponse result = service.getMyPage(MEMBER_ID);

            assertThat(result.scrapCount()).isZero();
            assertThat(result.remindCount()).isZero();
            assertThat(result.interestCount()).isZero();
            assertThat(result.departments()).isEmpty();
        }
    }
}
