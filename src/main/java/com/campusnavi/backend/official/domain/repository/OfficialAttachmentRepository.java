package com.campusnavi.backend.official.domain.repository;

import com.campusnavi.backend.official.domain.entity.OfficialAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OfficialAttachmentRepository extends JpaRepository<OfficialAttachment,Long> {
    List<OfficialAttachment> findByPostId(Long postId);
    List<OfficialAttachment> findByPostIdIn(List<Long> postIds);
}
