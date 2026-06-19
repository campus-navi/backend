package com.campusnavi.backend.studio.document.repository;

import com.campusnavi.backend.studio.document.entity.DocumentAiFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentAiFeedbackRepository extends JpaRepository<DocumentAiFeedback, Long> {
}
