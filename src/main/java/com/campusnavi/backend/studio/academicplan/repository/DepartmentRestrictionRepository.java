package com.campusnavi.backend.studio.academicplan.repository;

import com.campusnavi.backend.studio.academicplan.entity.DepartmentRestriction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DepartmentRestrictionRepository extends JpaRepository<DepartmentRestriction, Long> {

    @Query("SELECT dr.toDepartment.id FROM DepartmentRestriction dr " +
            "WHERE dr.fromCampus.id = :campusId AND dr.fromDepartment IS NULL AND dr.restrictDoubleMajor = true")
    List<Long> findDoubleRestrictedIdsByFromCampus(@Param("campusId") Long campusId);

    @Query("SELECT dr.toDepartment.id FROM DepartmentRestriction dr " +
            "WHERE dr.fromCampus.id = :campusId AND dr.fromDepartment IS NULL AND dr.restrictComplexMajor = true")
    List<Long> findComplexRestrictedIdsByFromCampus(@Param("campusId") Long campusId);

    @Query("SELECT dr.toDepartment.id FROM DepartmentRestriction dr " +
            "WHERE dr.fromDepartment.id = :departmentId AND dr.fromCampus IS NULL AND dr.restrictDoubleMajor = true")
    List<Long> findDoubleRestrictedIdsByFromDepartment(@Param("departmentId") Long departmentId);

    @Query("SELECT dr.toDepartment.id FROM DepartmentRestriction dr " +
            "WHERE dr.fromDepartment.id = :departmentId AND dr.fromCampus IS NULL AND dr.restrictComplexMajor = true")
    List<Long> findComplexRestrictedIdsByFromDepartment(@Param("departmentId") Long departmentId);
}
