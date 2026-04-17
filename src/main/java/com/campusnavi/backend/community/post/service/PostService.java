package com.campusnavi.backend.community.post.service;

import com.campusnavi.backend.community.post.dto.PostCreateRequest;
import com.campusnavi.backend.community.post.dto.PostCreateResponse;
import com.campusnavi.backend.community.post.dto.PostPresignedUrlRequest;
import com.campusnavi.backend.community.post.dto.PostResponse;
import com.campusnavi.backend.community.post.dto.PostSummaryResponse;
import com.campusnavi.backend.community.post.dto.PostUpdateRequest;
import com.campusnavi.backend.community.post.dto.ViewType;
import com.campusnavi.backend.global.response.CursorPageResponse;
import com.campusnavi.backend.community.post.entity.Post;
import com.campusnavi.backend.community.post.entity.PostImage;
import com.campusnavi.backend.community.post.repository.PostImageRepository;
import com.campusnavi.backend.community.post.repository.PostLikeRepository;
import com.campusnavi.backend.community.post.repository.PostRepository;
import com.campusnavi.backend.community.post.repository.PostScrapRepository;
import com.campusnavi.backend.global.exception.BusinessException;
import com.campusnavi.backend.global.exception.ErrorCode;
import com.campusnavi.backend.global.security.AuthMember;
import com.campusnavi.backend.infra.storage.PresignedUrlResponse;
import com.campusnavi.backend.infra.storage.S3StorageService;
import com.campusnavi.backend.infra.storage.UploadType;
import com.campusnavi.backend.member.entity.Member;
import com.campusnavi.backend.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final PostImageRepository imageRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostScrapRepository postScrapRepository;
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
        boolean isLiked = postLikeRepository.findByMemberIdAndPostId(authMember.memberId(), postId).isPresent();
        boolean isScraped = postScrapRepository.findByMemberIdAndPostId(authMember.memberId(), postId).isPresent();

        return new PostResponse(nickname, post.getTitle(), post.getContent(), post.getCreatedAt(),
                post.getLikeCount(), post.getCommentCount(), post.getScrapCount(),
                imageUrls, isLiked, isScraped, isMine);
    }

    @Transactional
    public void updatePost(Long postId, AuthMember authMember, PostUpdateRequest request) {
        Post post = postRepository.findByIdWithMember(postId, authMember.universityId())
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        if (!post.getMember().getId().equals(authMember.memberId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        post.update(request.title(), request.content(), request.isAnonymous());

        if (request.imageKeys() != null) {
            List<PostImage> existingImages = imageRepository.findByPostId(postId);
            try {
                for (PostImage image : existingImages) {
                    s3StorageService.delete(image.getImageKey());
                }
            } catch (Exception e) {
                log.error("S3 삭제처리중 예외발생 {}", e.getMessage());
            }
            imageRepository.deleteByPostId(postId);
            savePostImages(post, request.imageKeys());
        }
    }

    @Transactional
    public void deletePost(Long postId, AuthMember authMember) {
        Post post = postRepository.findByIdWithMember(postId, authMember.universityId())
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        if (!post.getMember().getId().equals(authMember.memberId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        List<PostImage> existingImages = imageRepository.findByPostId(postId);
        try {
            for (PostImage image : existingImages) {
                s3StorageService.delete(image.getImageKey());
            }
        } catch (Exception e) {
            log.error("S3 삭제처리중 예외 발생 {}", e.getMessage());
        }

        imageRepository.deleteByPostId(postId);
        post.softDelete();
    }

    public CursorPageResponse<PostSummaryResponse> getPosts(AuthMember authMember, ViewType viewType, String cursor, int size) {
        Long cursorId = decodeCursorId(cursor);
        Integer cursorScrapCount = decodeCursorScrapCount(cursor);

        List<Post> posts = switch (viewType) {
            case LATEST -> postRepository.findLatestPosts(authMember.universityId(), cursorId, size + 1);
            case POPULAR -> postRepository.findPopularPosts(authMember.universityId(), cursorId, size + 1);
            case SCRAP -> postRepository.findScrapPosts(authMember.universityId(), cursorId, cursorScrapCount, size + 1);
        };

        boolean hasNext = posts.size() > size;
        List<Post> result = hasNext ? posts.subList(0, size) : posts;

        String nextCursor = null;
        if (hasNext) {
            Post last = result.getLast();
            nextCursor = viewType == ViewType.SCRAP
                    ? encodeCursor(last.getId(), last.getScrapCount())
                    : encodeCursor(last.getId(), null);
        }

        List<Long> postIds = result.stream().map(Post::getId).toList();
        Set<Long> likedIds = new HashSet<>(postLikeRepository.findLikedPostIds(authMember.memberId(), postIds));
        Set<Long> scrapedIds = new HashSet<>(postScrapRepository.findScrapedPostIds(authMember.memberId(), postIds));

        List<PostSummaryResponse> summaries = result.stream()
                .map(post -> toSummary(post, likedIds.contains(post.getId()), scrapedIds.contains(post.getId())))
                .toList();
        return CursorPageResponse.of(summaries, nextCursor, hasNext);
    }

    private PostSummaryResponse toSummary(Post post, boolean isLiked, boolean isScraped) {
        String nickname = post.isAnonymous() ? "익명" : post.getMember().getNickname();
        String preview = post.getContent().length() > 100
                ? post.getContent().substring(0, 100)
                : post.getContent();
        return new PostSummaryResponse(
                post.getId(), nickname, post.getTitle(), preview,
                post.getLikeCount(), post.getScrapCount(), post.getCommentCount(), post.getCreatedAt(),
                isLiked, isScraped
        );
    }

    private String encodeCursor(Long postId, Integer scrapCount) {
        String raw = scrapCount != null ? postId + ":" + scrapCount : String.valueOf(postId);
        return Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private Long decodeCursorId(String cursor) {
        if (cursor == null) return null;
        String raw = new String(Base64.getDecoder().decode(cursor), StandardCharsets.UTF_8);
        return Long.parseLong(raw.split(":")[0]);
    }

    private Integer decodeCursorScrapCount(String cursor) {
        if (cursor == null) return null;
        String raw = new String(Base64.getDecoder().decode(cursor), StandardCharsets.UTF_8);
        String[] parts = raw.split(":");
        return parts.length == 2 ? Integer.parseInt(parts[1]) : null;
    }

    public PresignedUrlResponse generatePostPresignedUrl(PostPresignedUrlRequest request) {
        if (!request.contentType().startsWith("image/")) {
            throw new BusinessException(ErrorCode.INVALID_CONTENT_TYPE);
        }
        return s3StorageService.generatePresignedUrl(UploadType.POST_IMAGE, request.filename(), request.contentType(), request.size());
    }

    private void savePostImages(Post post, List<String> imageKeys) {
        if (imageKeys == null || imageKeys.isEmpty()) return;
        List<PostImage> images = new ArrayList<>();
        for (int i = 0; i < imageKeys.size(); i++) {
            images.add(PostImage.create(post, imageKeys.get(i), (short) i));
        }
        imageRepository.saveAll(images);
    }
}
