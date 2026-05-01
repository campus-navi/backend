package com.campusnavi.backend.official.post.repository;

import com.campusnavi.backend.official.post.entity.OfficialAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OfficialAttachmentRepository extends JpaRepository<OfficialAttachment,Long> {
    List<OfficialAttachment> findByPostId(Long postId);
    List<OfficialAttachment> findByPostIdIn(List<Long> postIds);
    List<OfficialAttachment> findByPostIdOrderBySortOrderAsc(Long postId);
}
