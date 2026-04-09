package com.campusnavi.backend.university.repository;

import com.campusnavi.backend.university.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department,Long> {
}
