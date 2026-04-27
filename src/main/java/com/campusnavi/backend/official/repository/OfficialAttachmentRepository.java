package com.campusnavi.backend.official.repository;

import com.campusnavi.backend.official.entity.OfficialAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OfficialAttachmentRepository extends JpaRepository<OfficialAttachment,Long> {
    List<OfficialAttachment> findByPostId(Long postId);
    List<OfficialAttachment> findByPostIdIn(List<Long> postIds);
}
