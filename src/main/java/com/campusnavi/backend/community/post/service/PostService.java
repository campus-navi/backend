package com.campusnavi.backend.community.post.service;

import com.campusnavi.backend.community.post.dto.PostCreateRequest;
import com.campusnavi.backend.community.post.dto.PostCreateResponse;
import com.campusnavi.backend.community.post.entity.Post;
import com.campusnavi.backend.community.post.entity.PostImage;
import com.campusnavi.backend.community.post.repository.PostImageRepository;
import com.campusnavi.backend.community.post.repository.PostRepository;
import com.campusnavi.backend.global.exception.BusinessException;
import com.campusnavi.backend.global.exception.ErrorCode;
import com.campusnavi.backend.member.entity.Member;
import com.campusnavi.backend.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final PostImageRepository imageRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public PostCreateResponse createPost(Long memberId, PostCreateRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        Post post = Post.create(member.getUniversityId(), member,
                request.title(), request.content(), request.isAnonymous());

        List<String> imageKeys = request.imageKeys();

        if (imageKeys != null) {
            for (String imageKey : imageKeys) {
                PostImage image = PostImage.create(post, imageKey, (short) imageKeys.indexOf(imageKey));
                imageRepository.save(image);
            }
        }

        postRepository.save(post);
        return new PostCreateResponse(post.getId());
    }

}
