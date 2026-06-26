package com.campusnavi.backend.studio.document.repository;

import com.campusnavi.backend.studio.document.entity.StudioDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudioDocumentRepository extends JpaRepository<StudioDocument, Long> {

    List<StudioDocument> findByMemberIdOrderByUpdatedAtDesc(Long memberId);
}
