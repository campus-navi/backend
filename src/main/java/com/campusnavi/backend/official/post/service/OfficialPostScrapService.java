package com.campusnavi.backend.official.post.service;

import com.campusnavi.backend.global.common.AuthContext;
import com.campusnavi.backend.global.exception.BusinessException;
import com.campusnavi.backend.global.exception.ErrorCode;
import com.campusnavi.backend.official.post.dto.OfficialPostScrapFolderResponse;
import com.campusnavi.backend.official.post.entity.OfficialPost;
import com.campusnavi.backend.official.post.entity.OfficialPostScrap;
import com.campusnavi.backend.official.post.repository.OfficialPostRepository;
import com.campusnavi.backend.official.post.repository.OfficialPostScrapRepository;
import com.campusnavi.backend.scrap.repository.ScrapFolderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
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
                .forEach(scrapRepository::delete);

        targetFolderIds.stream()
                .filter(folderId -> !currentFolderIds.contains(folderId))
                .forEach(folderId -> scrapRepository.save(
                        OfficialPostScrap.create(context.memberId(), post, folderId)));
    }
}
