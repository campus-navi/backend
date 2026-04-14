package com.campusnavi.backend.university.repository;

import com.campusnavi.backend.university.entity.College;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CollegeRepository extends JpaRepository<College, Long> {
}
