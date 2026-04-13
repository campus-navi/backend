package com.campusnavi.backend.university.repository;

import com.campusnavi.backend.university.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department,Long> {
    List<Department> findByCampusIdOrderByNameAsc(Long campusId);

    @Query("SELECT d FROM Department d JOIN FETCH d.campus c JOIN FETCH c.university WHERE d.id = :id")
    Optional<Department> findByIdWithCampusAndUniversity(@Param("id") Long id);
}
