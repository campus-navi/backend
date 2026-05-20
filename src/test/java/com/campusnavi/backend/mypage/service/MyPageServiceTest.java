package com.campusnavi.backend.mypage.service;

import com.campusnavi.backend.community.post.service.PostInteractionService;
import com.campusnavi.backend.global.exception.BusinessException;
import com.campusnavi.backend.global.exception.ErrorCode;
import com.campusnavi.backend.global.response.CursorPageResponse;
import com.campusnavi.backend.member.dto.MemberProfile;
import com.campusnavi.backend.member.service.MemberService;
import com.campusnavi.backend.mypage.dto.MyPageResponse;
import com.campusnavi.backend.mypage.dto.MyScrapResponse;
import com.campusnavi.backend.notification.service.RemindNotificationService;
import com.campusnavi.backend.official.post.dto.RecentScrapResponse;
import com.campusnavi.backend.official.post.dto.RecentViewResponse;
import com.campusnavi.backend.official.post.repository.OfficialPostViewRepository;
import com.campusnavi.backend.official.post.service.OfficialPostScrapService;
import com.campusnavi.backend.scrap.dto.ScrapFolderResponse;
import com.campusnavi.backend.scrap.dto.ScrapFolderSort;
import com.campusnavi.backend.scrap.service.ScrapFolderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

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

    @Mock
    private OfficialPostViewRepository officialPostViewRepository;

    @Mock
    private ScrapFolderService scrapFolderService;

    @InjectMocks
    private MyPageService service;

    private static final Long MEMBER_ID = 7L;
    private static final int PAGE_SIZE = 20;

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

    @Nested
    @DisplayName("최근 본 게시물 조회")
    class GetRecentViews {

        @Test
        @DisplayName("결과가 20건 이하면 hasNext=false, nextCursor=null")
        void singlePage() {
            List<RecentViewResponse> rows = List.of(
                    new RecentViewResponse(1L, "공고", "수강", LocalDate.now(), LocalDateTime.now()));
            given(officialPostViewRepository.findRecentViews(eq(MEMBER_ID), any(), any(Pageable.class)))
                    .willReturn(rows);

            CursorPageResponse<RecentViewResponse> result = service.getRecentViews(MEMBER_ID, null);

            assertThat(result.content()).hasSize(1);
            assertThat(result.hasNext()).isFalse();
            assertThat(result.nextCursor()).isNull();
        }

        @Test
        @DisplayName("결과가 PAGE_SIZE+1건이면 hasNext=true, nextCursor 발급")
        void multiplePages() {
            LocalDateTime baseTime = LocalDateTime.of(2026, 5, 20, 12, 0);
            List<RecentViewResponse> rows = IntStream.rangeClosed(1, PAGE_SIZE + 1)
                    .mapToObj(i -> new RecentViewResponse((long) i, "제목" + i, "수강", null,
                            baseTime.minusMinutes(i)))
                    .toList();
            given(officialPostViewRepository.findRecentViews(eq(MEMBER_ID), any(), any(Pageable.class)))
                    .willReturn(rows);

            CursorPageResponse<RecentViewResponse> result = service.getRecentViews(MEMBER_ID, null);

            assertThat(result.content()).hasSize(PAGE_SIZE);
            assertThat(result.hasNext()).isTrue();
            assertThat(result.nextCursor()).isNotNull();
        }

        @Test
        @DisplayName("빈 결과면 content=[], hasNext=false")
        void emptyResult() {
            given(officialPostViewRepository.findRecentViews(eq(MEMBER_ID), any(), any(Pageable.class)))
                    .willReturn(List.of());

            CursorPageResponse<RecentViewResponse> result = service.getRecentViews(MEMBER_ID, null);

            assertThat(result.content()).isEmpty();
            assertThat(result.hasNext()).isFalse();
        }

        @Test
        @DisplayName("유효한 cursor를 전달하면 파싱 후 조회한다")
        void validCursor() {
            LocalDateTime cursorTime = LocalDateTime.of(2026, 5, 20, 12, 0);
            given(officialPostViewRepository.findRecentViews(eq(MEMBER_ID), eq(cursorTime), any(Pageable.class)))
                    .willReturn(List.of());

            service.getRecentViews(MEMBER_ID, cursorTime.toString());

            then(officialPostViewRepository).should()
                    .findRecentViews(eq(MEMBER_ID), eq(cursorTime), any(Pageable.class));
        }

        @Test
        @DisplayName("잘못된 cursor면 INVALID_PARAM 예외")
        void invalidCursor() {
            assertThatThrownBy(() -> service.getRecentViews(MEMBER_ID, "not-iso"))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_PARAM));
        }
    }

    @Nested
    @DisplayName("최근 본 게시물 삭제")
    class DeleteRecentView {

        @Test
        @DisplayName("Repository의 deleteByMemberIdAndPostId를 호출한다")
        void success() {
            service.deleteRecentView(MEMBER_ID, 5L);

            then(officialPostViewRepository).should().deleteByMemberIdAndPostId(MEMBER_ID, 5L);
        }
    }

    @Nested
    @DisplayName("내 스크랩 화면 조회")
    class GetMyScraps {

        @Test
        @DisplayName("최근 스크랩과 폴더 목록을 조립해 반환한다")
        void success() {
            List<RecentScrapResponse> recent = List.of(
                    new RecentScrapResponse(2L, "장학 공고", "장학", null, null));
            List<ScrapFolderResponse> folders = List.of(
                    new ScrapFolderResponse(100L, "취업", null, 1L));
            given(officialPostScrapService.getRecentScraps(MEMBER_ID)).willReturn(recent);
            given(scrapFolderService.getFolders(MEMBER_ID, ScrapFolderSort.RECENT_SAVED))
                    .willReturn(folders);

            MyScrapResponse result = service.getMyScraps(MEMBER_ID);

            assertThat(result.recentScraps()).isEqualTo(recent);
            assertThat(result.folders()).isEqualTo(folders);
        }

        @Test
        @DisplayName("최근 스크랩과 폴더가 없으면 빈 목록을 반환한다")
        void empty() {
            given(officialPostScrapService.getRecentScraps(MEMBER_ID)).willReturn(List.of());
            given(scrapFolderService.getFolders(MEMBER_ID, ScrapFolderSort.RECENT_SAVED))
                    .willReturn(List.of());

            MyScrapResponse result = service.getMyScraps(MEMBER_ID);

            assertThat(result.recentScraps()).isEmpty();
            assertThat(result.folders()).isEmpty();
        }
    }
}
