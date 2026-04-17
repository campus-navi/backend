package com.campusnavi.backend.university.repository;

import com.campusnavi.backend.university.entity.University;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UniversityRepository extends JpaRepository<University,Long> {
}
