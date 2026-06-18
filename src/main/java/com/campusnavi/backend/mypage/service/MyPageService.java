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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageService {

    private static final int RECENT_VIEW_PAGE_SIZE = 20;

    private final MemberService memberService;
    private final OfficialPostScrapService officialPostScrapService;
    private final PostInteractionService postInteractionService;
    private final RemindNotificationService remindNotificationService;
    private final OfficialPostViewRepository officialPostViewRepository;
    private final ScrapFolderService scrapFolderService;

    public MyPageResponse getMyPage(Long memberId) {
        MemberProfile profile = memberService.getMyProfile(memberId);

        long scrapCount = officialPostScrapService.countScrappedPosts(memberId)
                + postInteractionService.countScraps(memberId);
        long remindCount = remindNotificationService.getActiveRemindCount(memberId);
        long interestCount = memberService.countMyInterests(memberId);

        return new MyPageResponse(
                profile.name(),
                profile.nickname(),
                profile.email(),
                profile.campus(),
                profile.studentNumber(),
                profile.admissionYear(),
                profile.grade(),
                profile.departments(),
                scrapCount,
                remindCount,
                interestCount);
    }

    public CursorPageResponse<RecentViewResponse> getRecentViews(Long memberId, String cursor) {
        LocalDateTime cursorLastViewedAt = null;

        if (cursor != null) {
            try {
                cursorLastViewedAt = LocalDateTime.parse(cursor);
            } catch (Exception e) {
                throw new BusinessException(ErrorCode.INVALID_PARAM);
            }
        }

        List<RecentViewResponse> rows = officialPostViewRepository
                .findRecentViews(memberId, cursorLastViewedAt, PageRequest.ofSize(RECENT_VIEW_PAGE_SIZE + 1));

        boolean hasNext = rows.size() > RECENT_VIEW_PAGE_SIZE;
        List<RecentViewResponse> content = hasNext
                ? rows.subList(0, RECENT_VIEW_PAGE_SIZE)
                : rows;

        String nextCursor = hasNext
                ? content.getLast().lastViewedAt().toString()
                : null;

        return CursorPageResponse.of(content, nextCursor, hasNext);
    }

    @Transactional
    public void deleteRecentView(Long memberId, Long postId) {
        officialPostViewRepository.deleteByMemberIdAndPostId(memberId, postId);
    }

    public MyScrapResponse getMyScraps(Long memberId) {
        List<RecentScrapResponse> recentScraps = officialPostScrapService.getRecentScraps(memberId);
        List<ScrapFolderResponse> folders =
                scrapFolderService.getFolders(memberId, ScrapFolderSort.RECENT_SAVED);
        return new MyScrapResponse(recentScraps, folders);
    }
}
