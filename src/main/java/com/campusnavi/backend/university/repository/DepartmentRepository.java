package com.campusnavi.backend.university.repository;

import com.campusnavi.backend.university.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DepartmentRepository extends JpaRepository<Department,Long> {
    List<Department> findByCampusIdOrderByNameAsc(Long campusId);
}
