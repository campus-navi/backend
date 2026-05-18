package com.campusnavi.backend.mypage.service;

import com.campusnavi.backend.community.post.service.PostInteractionService;
import com.campusnavi.backend.member.dto.MemberProfile;
import com.campusnavi.backend.member.service.MemberService;
import com.campusnavi.backend.mypage.dto.MyPageResponse;
import com.campusnavi.backend.notification.service.RemindNotificationService;
import com.campusnavi.backend.official.post.service.OfficialPostScrapService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageService {

    private final MemberService memberService;
    private final OfficialPostScrapService officialPostScrapService;
    private final PostInteractionService postInteractionService;
    private final RemindNotificationService remindNotificationService;

    public MyPageResponse getMyPage(Long memberId) {
        MemberProfile profile = memberService.getMyProfile(memberId);

        long scrapCount = officialPostScrapService.countScrappedPosts(memberId)
                + postInteractionService.countScraps(memberId);
        long remindCount = remindNotificationService.getActiveRemindCount(memberId);
        long interestCount = memberService.countMyInterests(memberId);

        return new MyPageResponse(
                profile.nickname(),
                profile.email(),
                profile.campus(),
                profile.admissionYear(),
                profile.grade(),
                profile.departments(),
                scrapCount,
                remindCount,
                interestCount);
    }
}
