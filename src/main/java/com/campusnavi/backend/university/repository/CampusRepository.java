package com.campusnavi.backend.university.repository;

import com.campusnavi.backend.university.entity.Campus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampusRepository extends JpaRepository<Campus,Long> {
}
