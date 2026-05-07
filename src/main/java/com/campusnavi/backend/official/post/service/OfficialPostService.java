package com.campusnavi.backend.official.post.service;

import com.campusnavi.backend.global.common.AuthContext;
import com.campusnavi.backend.global.common.ProcessingStatus;
import com.campusnavi.backend.global.exception.BusinessException;
import com.campusnavi.backend.global.exception.ErrorCode;
import com.campusnavi.backend.global.response.CursorPageResponse;
import com.campusnavi.backend.infra.storage.S3StorageService;
import com.campusnavi.backend.member.dto.MemberScope;
import com.campusnavi.backend.member.repository.MemberQueryRepository;
import com.campusnavi.backend.official.post.dto.*;
import com.campusnavi.backend.official.post.entity.OfficialAttachment;
import com.campusnavi.backend.official.post.entity.OfficialPost;
import com.campusnavi.backend.official.post.entity.OfficialPostAiMeta;
import com.campusnavi.backend.official.post.repository.*;
import com.campusnavi.backend.tag.entity.Tag;
import com.campusnavi.backend.tag.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OfficialPostService {

    private final OfficialPostRepository postRepository;
    private final OfficialPostAiMetaRepository aiMetaRepository;
    private final OfficialAttachmentRepository attachmentRepository;
    private final OfficialAttachmentDownloadRepository downloadRepository;
    private final OfficialPostScrapRepository scrapRepository;
    private final OfficialPostNotificationRepository notificationRepository;
    private final OfficialPostViewService viewService;
    private final S3StorageService storageService;
    private final MemberQueryRepository memberQueryRepository;
    private final OfficialPostQueryRepository officialPostQueryRepository;
    private final TagRepository tagRepository;

    private static final int PAGE_SIZE = 20;
    private static final int MAX_KEYWORD_LENGTH = 100;

    public OfficialPostDetailResponse getDetail(Long postId, AuthContext context) {
        OfficialPost post = postRepository.findActiveByIdAndUniversityScope(postId, context.universityId())
                .orElseThrow(() -> new BusinessException(ErrorCode.OFFICIAL_POST_NOT_FOUND));

        OfficialPostAiMeta meta = aiMetaRepository.findByOfficialPostIdWithTag(postId, ProcessingStatus.DONE)
                .orElseThrow(() -> new BusinessException(ErrorCode.OFFICIAL_POST_NOT_READY));

        List<OfficialAttachment> all = attachmentRepository.findByPostIdOrderBySortOrderAsc(postId);

        String thumbnailUrl = all.stream()
                .filter(OfficialAttachment::isImage)
                .findFirst()
                .map(a -> storageService.resolveUrl(a.getS3Key()))
                .orElse(null);

        List<OfficialAttachment> nonImages = all.stream()
                .filter(a -> !a.isImage())
                .toList();

        Set<Long> downloadedIds = nonImages.isEmpty()
                ? Set.of()
                : downloadRepository.findDownloadedAttachmentIds(
                context.memberId(),
                nonImages.stream().map(OfficialAttachment::getId).toList());

        List<AttachmentResponse> files = nonImages.stream()
                .map(a -> new AttachmentResponse(
                        a.getId(),
                        a.getOriginalName(),
                        downloadedIds.contains(a.getId())))
                .toList();

        boolean hasUnreadAttachments = nonImages.size() > downloadedIds.size();

        boolean isScrapped = scrapRepository.existsByMemberIdAndPostId(context.memberId(), postId);

        boolean isNotificationOn = notificationRepository.existsByMemberIdAndPostId(context.memberId(), postId);

        viewService.recordView(context.memberId(), postId);

        return new OfficialPostDetailResponse(
                post.getId(),
                post.getTitle(),
                post.getPublisher(),
                post.getSourceUrl(),
                post.getPublishedAt(),
                meta.getTag() != null ? meta.getTag().getName() : null,
                meta.getSummary(),
                post.getHtmlContent(),
                meta.isApplicable(),
                meta.getStartDate(),
                meta.getStartTime(),
                meta.getEndDate(),
                meta.getEndTime(),
                meta.getEligibility(),
                meta.getApplyMethodType(),
                meta.getApplyMethodDetail(),
                meta.getRequiredDocuments(),
                meta.getContactPhone(),
                meta.getContactEmail(),
                thumbnailUrl,
                files,
                hasUnreadAttachments,
                isScrapped,
                isNotificationOn
        );
    }

    public CursorPageResponse<OfficialPostSummaryResponse> getList(
            AuthContext context, String q, String tagCode, OfficialPostListSort sort, String cursor) {

        if (q != null && q.length() > MAX_KEYWORD_LENGTH) {
            throw new BusinessException(ErrorCode.INVALID_PARAM);
        }

        OfficialPostScopeCondition condition = toCondition(context);

        Long tagId = (tagCode == null || tagCode.isBlank())
                ? null
                : tagRepository.findByCode(tagCode)
                        .map(Tag::getId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_PARAM));

        LocalDate latestCursorPublishedAt = null;
        Long latestCursorId = null;
        LocalDate deadlineCursorDate = null;
        Long deadlineCursorId = null;

        if (cursor != null) {
            String raw;
            try {
                raw = new String(Base64.getDecoder().decode(cursor), StandardCharsets.UTF_8);
            } catch (Exception e) {
                throw new BusinessException(ErrorCode.INVALID_PARAM);
            }
            if (sort == OfficialPostListSort.LATEST) {
                try {
                    String[] parts = raw.split(":", 2);
                    latestCursorPublishedAt = "null".equals(parts[0]) ? null : LocalDate.parse(parts[0]);
                    latestCursorId = Long.parseLong(parts[1]);
                } catch (Exception e) {
                    throw new BusinessException(ErrorCode.INVALID_PARAM);
                }
            } else {
                try {
                    String[] parts = raw.split(":", 2);
                    deadlineCursorDate = LocalDate.parse(parts[0]);
                    deadlineCursorId = Long.parseLong(parts[1]);
                } catch (Exception e) {
                    throw new BusinessException(ErrorCode.INVALID_PARAM);
                }
            }
        }

        List<OfficialPostSummaryRaw> rows = officialPostQueryRepository
                .findList(condition, q, tagId, sort, latestCursorPublishedAt, latestCursorId, deadlineCursorDate, deadlineCursorId, PAGE_SIZE + 1);

        boolean hasNext = rows.size() > PAGE_SIZE;
        List<OfficialPostSummaryRaw> page = hasNext ? rows.subList(0, PAGE_SIZE) : rows;

        String nextCursor = null;
        if (hasNext) {
            OfficialPostSummaryRaw last = page.getLast();
            if (sort == OfficialPostListSort.LATEST) {
                String raw = last.publishedAt() + ":" + last.postId();
                nextCursor = Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
            } else {
                String raw = last.endDate() + ":" + last.postId();
                nextCursor = Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
            }
        }

        List<OfficialPostSummaryResponse> content = page.stream()
                .map(r -> new OfficialPostSummaryResponse(r.postId(), r.title(), r.tagName(), r.publishedAt(), r.endDate()))
                .toList();

        return CursorPageResponse.of(content, nextCursor, hasNext);
    }

    private OfficialPostScopeCondition toCondition(AuthContext context) {
        List<MemberScope> memberScopes = memberQueryRepository.findScopesByMemberId(context.memberId());
        return OfficialPostScopeCondition.from(context.universityId(), memberScopes);
    }
}
