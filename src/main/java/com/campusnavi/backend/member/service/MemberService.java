package com.campusnavi.backend.member.service;

import com.campusnavi.backend.global.exception.BusinessException;
import com.campusnavi.backend.global.exception.ErrorCode;
import com.campusnavi.backend.global.security.AuthMember;
import com.campusnavi.backend.tag.entity.Tag;
import com.campusnavi.backend.tag.repository.TagRepository;
import com.campusnavi.backend.member.dto.MemberInterestUpdateRequest;
import com.campusnavi.backend.member.dto.MemberMeResponse;
import com.campusnavi.backend.member.entity.Member;
import com.campusnavi.backend.member.entity.MemberInterest;
import com.campusnavi.backend.member.repository.MemberInterestRepository;
import com.campusnavi.backend.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final MemberInterestRepository memberInterestRepository;
    private final TagRepository tagRepository;

    @Transactional(readOnly = true)
    public MemberMeResponse getMe(AuthMember authMember) {
        Member member = memberRepository.findById(authMember.memberId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        boolean hasSetInterests = memberInterestRepository.existsByMemberId(authMember.memberId());
        return new MemberMeResponse(member.getNickname(), hasSetInterests);
    }

    @Transactional
    public void updateMemberInterests(AuthMember authMember, MemberInterestUpdateRequest request) {
        Long memberId = authMember.memberId();
        List<Long> interestIds = request.interestIds();

        memberInterestRepository.deleteAllByMemberId(memberId);

        if (interestIds.isEmpty()) {
            return;
        }

        List<Tag> tags = tagRepository.findAllById(interestIds);

        if (tags.size() != interestIds.size()) {
            throw new BusinessException(ErrorCode.TAG_NOT_FOUND);
        }

        List<MemberInterest> memberInterests = tags.stream()
                .map(tag -> MemberInterest.create(memberId, tag))
                .toList();

        memberInterestRepository.saveAll(memberInterests);
    }
}
