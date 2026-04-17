package com.campusnavi.backend.community.post.service;

import com.campusnavi.backend.community.post.dto.PostCreateRequest;
import com.campusnavi.backend.community.post.dto.PostCreateResponse;
import com.campusnavi.backend.community.post.dto.PostPresignedUrlRequest;
import com.campusnavi.backend.community.post.dto.PostResponse;
import com.campusnavi.backend.community.post.entity.Post;
import com.campusnavi.backend.community.post.entity.PostImage;
import com.campusnavi.backend.community.post.repository.PostImageRepository;
import com.campusnavi.backend.community.post.repository.PostRepository;
import com.campusnavi.backend.global.exception.BusinessException;
import com.campusnavi.backend.global.exception.ErrorCode;
import com.campusnavi.backend.global.security.AuthMember;
import com.campusnavi.backend.infra.storage.PresignedUrlResponse;
import com.campusnavi.backend.infra.storage.S3StorageService;
import com.campusnavi.backend.infra.storage.UploadType;
import com.campusnavi.backend.member.entity.Member;
import com.campusnavi.backend.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final PostImageRepository imageRepository;
    private final MemberRepository memberRepository;
    private final S3StorageService s3StorageService;

    @Transactional
    public PostCreateResponse createPost(AuthMember authMember, PostCreateRequest request) {
        Member member = memberRepository.findById(authMember.memberId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        Post post = Post.create(member.getUniversityId(), member,
                request.title(), request.content(), request.isAnonymous());

        postRepository.save(post);
        savePostImages(post, request.imageKeys());

        return new PostCreateResponse(post.getId());
    }

    public PostResponse getPost(Long postId, AuthMember authMember) {
        Post post = postRepository.findByIdWithMember(postId, authMember.universityId())
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        String nickname = post.getMember().getNickname();
        if (post.isAnonymous()) {
            nickname = "익명";
        }

        List<PostImage> images = imageRepository.findByPostIdOrderBySortOrderAsc(postId);
        List<String> imageUrls = new ArrayList<>();
        for (PostImage image : images) {
            imageUrls.add(s3StorageService.resolveUrl(image.getImageKey()));
        }

        boolean isMine = post.getMember().getId().equals(authMember.memberId());

        return new PostResponse(nickname, post.getTitle(), post.getContent(), post.getCreatedAt(),
                post.getLikeCount(), post.getCommentCount(), post.getScrapCount(),
                imageUrls, false, false, isMine);
    }

    public PresignedUrlResponse generatePostPresignedUrl(PostPresignedUrlRequest request) {
        if (!request.contentType().startsWith("image/")) {
            throw new BusinessException(ErrorCode.INVALID_CONTENT_TYPE);
        }
        return s3StorageService.generatePresignedUrl(UploadType.POST_IMAGE, request.filename(), request.contentType(), request.size());
    }

    private void savePostImages(Post post, List<String> imageKeys) {
        if (imageKeys != null) {
            for (int i = 0; i < imageKeys.size(); i++) {
                PostImage image = PostImage.create(post, imageKeys.get(i), (short) i);
                imageRepository.save(image);
            }
        }
    }
}
