package com.campusnavi.backend.scrap.service;

import com.campusnavi.backend.global.exception.BusinessException;
import com.campusnavi.backend.global.exception.ErrorCode;
import com.campusnavi.backend.scrap.dto.ScrapFolderCreateRequest;
import com.campusnavi.backend.scrap.dto.ScrapFolderResponse;
import com.campusnavi.backend.scrap.dto.ScrapFolderSort;
import com.campusnavi.backend.scrap.dto.ScrapFolderUpdateRequest;
import com.campusnavi.backend.scrap.entity.ScrapFolder;
import com.campusnavi.backend.scrap.repository.ScrapFolderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScrapFolderService {

    private static final long MAX_FOLDER_COUNT = 16;

    private final ScrapFolderRepository scrapFolderRepository;

    @Transactional
    public void create(Long memberId, ScrapFolderCreateRequest request) {
        // 동시 요청 시 한도가 일시 초과될 수 있으나, 발생 가능성이 낮고 정합성 위협이 아니므로 락 없이 수용
        if (scrapFolderRepository.countByMemberId(memberId) >= MAX_FOLDER_COUNT) {
            throw new BusinessException(ErrorCode.SCRAP_FOLDER_LIMIT_EXCEEDED);
        }
        if (scrapFolderRepository.existsByMemberIdAndName(memberId, request.name())) {
            throw new BusinessException(ErrorCode.SCRAP_FOLDER_NAME_DUPLICATE);
        }
        scrapFolderRepository.save(ScrapFolder.create(memberId, request.name(), request.description()));
    }

    @Transactional
    public void update(Long memberId, Long folderId, ScrapFolderUpdateRequest request) {
        ScrapFolder folder = scrapFolderRepository.findByIdAndMemberId(folderId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCRAP_FOLDER_NOT_FOUND));
        if (!folder.getName().equals(request.name())
                && scrapFolderRepository.existsByMemberIdAndName(memberId, request.name())) {
            throw new BusinessException(ErrorCode.SCRAP_FOLDER_NAME_DUPLICATE);
        }
        folder.update(request.name(), request.description());
    }

    @Transactional
    public void delete(Long memberId, Long folderId) {
        ScrapFolder folder = scrapFolderRepository.findByIdAndMemberId(folderId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCRAP_FOLDER_NOT_FOUND));
        scrapFolderRepository.delete(folder);
    }

    public List<ScrapFolderResponse> getFolders(Long memberId, ScrapFolderSort sort) {
        return scrapFolderRepository.findByMemberId(memberId, toSort(sort)).stream()
                .map(ScrapFolderResponse::of)
                .toList();
    }

    private Sort toSort(ScrapFolderSort sort) {
        return switch (sort) {
            case NAME_ASC -> Sort.by(Sort.Direction.ASC, "name");
            case NAME_DESC -> Sort.by(Sort.Direction.DESC, "name");
            case LIST_ADDED -> Sort.by(Sort.Direction.ASC, "createdAt");
            case RECENT_SAVED -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }
}
