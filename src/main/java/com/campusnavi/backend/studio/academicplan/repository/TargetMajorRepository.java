package com.campusnavi.backend.studio.academicplan.repository;

import com.campusnavi.backend.studio.academicplan.MajorType;
import com.campusnavi.backend.studio.academicplan.entity.TargetMajor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TargetMajorRepository extends JpaRepository<TargetMajor, Long> {
    List<TargetMajor> findByCampusIdAndMajorTypeOrderByNameAsc(Long campusId, MajorType majorType);
}
