package com.campusnavi.backend.member.repository;

import com.campusnavi.backend.member.entity.MemberDepartment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberDepartmentRepository extends JpaRepository<MemberDepartment,Long> {
}
