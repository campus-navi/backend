package com.campusnavi.backend.community.post.service;

import com.campusnavi.backend.community.post.entity.PostLike;
import com.campusnavi.backend.community.post.entity.PostScrap;
import com.campusnavi.backend.community.post.repository.PostLikeRepository;
import com.campusnavi.backend.community.post.repository.PostRepository;
import com.campusnavi.backend.community.post.repository.PostScrapRepository;
import com.campusnavi.backend.global.exception.BusinessException;
import com.campusnavi.backend.global.exception.ErrorCode;
import com.campusnavi.backend.global.security.AuthMember;
import com.campusnavi.backend.member.entity.Member;
import com.campusnavi.backend.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PostInteractionService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostScrapRepository postScrapRepository;
    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public long countScraps(Long memberId) {
        return postScrapRepository.countByMemberId(memberId);
    }

    public void addLike(Long postId, AuthMember authMember) {
        postRepository.findByIdWithMember(postId, authMember.universityId())
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
        Member member = memberRepository.findById(authMember.memberId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        if (postLikeRepository.findByMemberIdAndPostId(member.getId(), postId).isPresent()) {
            return;
        }
        postLikeRepository.save(PostLike.create(member, postRepository.getReferenceById(postId)));
        postRepository.incrementLikeCount(postId);
    }

    public void removeLike(Long postId, AuthMember authMember) {
        postRepository.findByIdWithMember(postId, authMember.universityId())
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        postLikeRepository.findByMemberIdAndPostId(authMember.memberId(), postId)
                .ifPresent(like -> {
                    postLikeRepository.delete(like);
                    postRepository.decrementLikeCount(postId);
                });
    }

    public void addScrap(Long postId, AuthMember authMember) {
        postRepository.findByIdWithMember(postId, authMember.universityId())
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
        Member member = memberRepository.findById(authMember.memberId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        if (postScrapRepository.findByMemberIdAndPostId(member.getId(), postId).isPresent()) {
            return;
        }
        postScrapRepository.save(PostScrap.create(member, postRepository.getReferenceById(postId)));
        postRepository.incrementScrapCount(postId);
    }

    public void removeScrap(Long postId, AuthMember authMember) {
        postRepository.findByIdWithMember(postId, authMember.universityId())
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        postScrapRepository.findByMemberIdAndPostId(authMember.memberId(), postId)
                .ifPresent(scrap -> {
                    postScrapRepository.delete(scrap);
                    postRepository.decrementScrapCount(postId);
                });
    }
}
