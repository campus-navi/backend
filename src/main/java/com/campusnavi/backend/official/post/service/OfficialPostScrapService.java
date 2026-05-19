package com.campusnavi.backend.official.post.service;

import com.campusnavi.backend.global.common.AuthContext;
import com.campusnavi.backend.global.exception.BusinessException;
import com.campusnavi.backend.global.exception.ErrorCode;
import com.campusnavi.backend.official.post.dto.FolderScrapResponse;
import com.campusnavi.backend.official.post.dto.OfficialPostScrapFolderResponse;
import com.campusnavi.backend.official.post.dto.RecentScrapResponse;
import com.campusnavi.backend.official.post.dto.ScrapBulkDeleteResponse;
import com.campusnavi.backend.official.post.entity.OfficialPost;
import com.campusnavi.backend.official.post.entity.OfficialPostScrap;
import com.campusnavi.backend.official.post.repository.OfficialPostRepository;
import com.campusnavi.backend.official.post.repository.OfficialPostScrapRepository;
import com.campusnavi.backend.scrap.repository.ScrapFolderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class OfficialPostScrapService {

    private final OfficialPostRepository postRepository;
    private final OfficialPostScrapRepository scrapRepository;
    private final ScrapFolderRepository scrapFolderRepository;

    @Transactional(readOnly = true)
    public long countScrappedPosts(Long memberId) {
        return scrapRepository.countDistinctPostByMemberId(memberId);
    }

    @Transactional(readOnly = true)
    public List<OfficialPostScrapFolderResponse> getScrapFolders(Long postId, AuthContext context) {
        if (!postRepository.existsActiveByIdAndUniversityScope(postId, context.universityId())) {
            throw new BusinessException(ErrorCode.OFFICIAL_POST_NOT_FOUND);
        }

        Set<Long> scrappedFolderIds = Set.copyOf(
                scrapRepository.findScrapFolderIdsByMemberIdAndPostId(context.memberId(), postId));

        return scrapFolderRepository.findByMemberId(context.memberId(), Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(folder -> new OfficialPostScrapFolderResponse(
                        folder.getId(), folder.getName(), scrappedFolderIds.contains(folder.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RecentScrapResponse> getRecentScraps(Long memberId) {
        List<Long> postIds = scrapRepository.findRecentScrappedPostIds(memberId, PageRequest.of(0, 8));
        if (postIds.isEmpty()) {
            return List.of();
        }
        Map<Long, RecentScrapResponse> byId = scrapRepository.findRecentScrapCards(postIds).stream()
                .collect(Collectors.toMap(RecentScrapResponse::postId, Function.identity()));
        return postIds.stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FolderScrapResponse> getFolderScraps(Long folderId, AuthContext context) {
        scrapFolderRepository.findByIdAndMemberId(folderId, context.memberId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SCRAP_FOLDER_NOT_FOUND));
        return scrapRepository.findFolderScraps(context.memberId(), folderId);
    }

    public void setScrapFolders(Long postId, List<Long> folderIds, AuthContext context) {
        OfficialPost post = postRepository.findActiveByIdAndUniversityScope(postId, context.universityId())
                .orElseThrow(() -> new BusinessException(ErrorCode.OFFICIAL_POST_NOT_FOUND));

        Set<Long> targetFolderIds = Set.copyOf(folderIds);
        if (scrapFolderRepository.findAllByIdInAndMemberId(targetFolderIds, context.memberId()).size()
                != targetFolderIds.size()) {
            throw new BusinessException(ErrorCode.SCRAP_FOLDER_NOT_FOUND);
        }

        List<OfficialPostScrap> current = scrapRepository.findByMemberIdAndPostId(context.memberId(), postId);
        Set<Long> currentFolderIds = current.stream()
                .map(OfficialPostScrap::getScrapFolderId)
                .collect(Collectors.toSet());

        current.stream()
                .filter(scrap -> !targetFolderIds.contains(scrap.getScrapFolderId()))
                .forEach(scrap -> {
                    scrapRepository.delete(scrap);
                    scrapFolderRepository.decrementScrapCount(scrap.getScrapFolderId());
                });

        targetFolderIds.stream()
                .filter(folderId -> !currentFolderIds.contains(folderId))
                .forEach(folderId -> {
                    scrapRepository.save(OfficialPostScrap.create(context.memberId(), post, folderId));
                    scrapFolderRepository.incrementScrapCount(folderId);
                });
    }

    public ScrapBulkDeleteResponse deleteScraps(Long folderId, List<Long> scrapIds, AuthContext context) {
        scrapFolderRepository.findByIdAndMemberId(folderId, context.memberId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SCRAP_FOLDER_NOT_FOUND));

        Set<Long> ids = Set.copyOf(scrapIds);
        List<Long> deletedPostIds = scrapRepository.findScrappedPostIds(ids, context.memberId(), folderId);
        if (deletedPostIds.isEmpty()) {
            return new ScrapBulkDeleteResponse(0, List.of());
        }

        int deletedCount = scrapRepository.deleteScrapsByIds(ids, context.memberId(), folderId);
        scrapFolderRepository.decrementScrapCount(folderId, deletedCount);

        return new ScrapBulkDeleteResponse(deletedCount, deletedPostIds);
    }

    public void restoreScraps(Long folderId, List<Long> postIds, AuthContext context) {
        scrapFolderRepository.findByIdAndMemberId(folderId, context.memberId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SCRAP_FOLDER_NOT_FOUND));

        Set<Long> targetPostIds = Set.copyOf(postIds);
        Set<Long> alreadyScrapped = Set.copyOf(
                scrapRepository.findExistingPostIds(context.memberId(), folderId, targetPostIds));

        List<OfficialPostScrap> toSave = postRepository
                .findByIdInAndUniversityScope(targetPostIds, context.universityId()).stream()
                .filter(post -> !alreadyScrapped.contains(post.getId()))
                .map(post -> OfficialPostScrap.create(context.memberId(), post, folderId))
                .toList();
        if (toSave.isEmpty()) {
            return;
        }
        scrapRepository.saveAll(toSave);
        scrapFolderRepository.incrementScrapCount(folderId, toSave.size());
    }
}
