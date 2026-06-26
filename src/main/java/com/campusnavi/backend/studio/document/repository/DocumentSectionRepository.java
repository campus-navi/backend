package com.campusnavi.backend.studio.document.repository;

import com.campusnavi.backend.studio.document.entity.DocumentSection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentSectionRepository extends JpaRepository<DocumentSection, Long> {

    List<DocumentSection> findByDocumentIdOrderBySortOrderAsc(Long documentId);
}
