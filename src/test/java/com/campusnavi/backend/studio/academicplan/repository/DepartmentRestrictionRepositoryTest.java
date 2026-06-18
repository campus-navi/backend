package com.campusnavi.backend.studio.academicplan.repository;

import com.campusnavi.backend.studio.academicplan.entity.DepartmentRestriction;
import com.campusnavi.backend.support.PostgresSliceTestSupport;
import com.campusnavi.backend.university.entity.Campus;
import com.campusnavi.backend.university.entity.Department;
import com.campusnavi.backend.university.entity.University;
import com.campusnavi.backend.university.repository.CampusRepository;
import com.campusnavi.backend.university.repository.DepartmentRepository;
import com.campusnavi.backend.university.repository.UniversityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class DepartmentRestrictionRepositoryTest extends PostgresSliceTestSupport {

    @Autowired
    private DepartmentRestrictionRepository departmentRestrictionRepository;

    @Autowired
    private UniversityRepository universityRepository;

    @Autowired
    private CampusRepository campusRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    private Campus campus;
    private Department dept1;
    private Department dept2;
    private Department dept3;
    private Department fromDept;

    @BeforeEach
    void setUp() {
        University university = universityRepository.save(University.create("테스트대학교"));
        campus = campusRepository.save(Campus.create(university, "서울캠퍼스", "TEST", "test.ac.kr"));

        dept1    = departmentRepository.save(Department.create(campus, "학과A"));
        dept2    = departmentRepository.save(Department.create(campus, "학과B"));
        dept3    = departmentRepository.save(Department.create(campus, "학과C"));
        fromDept = departmentRepository.save(Department.create(campus, "경영학과"));

        departmentRestrictionRepository.saveAll(List.of(
                DepartmentRestriction.ofCampus(campus, dept1, true,  false), // Row A
                DepartmentRestriction.ofCampus(campus, dept2, false, true),  // Row B
                DepartmentRestriction.ofDepartment(fromDept, dept1, false, true),  // Row C
                DepartmentRestriction.ofDepartment(fromDept, dept3, true,  false)  // Row D
        ));
    }

    @Test
    @DisplayName("캠퍼스 공통 이중전공 불가 학과 ID를 반환한다")
    void findDoubleRestrictedByFromCampus() {
        // when
        List<Long> result = departmentRestrictionRepository.findDoubleRestrictedIdsByFromCampus(campus.getId());

        // then: Row A만 해당 (Row B는 플래그 불일치, Row C/D는 fromDepartment IS NOT NULL)
        assertThat(result).containsExactly(dept1.getId());
    }

    @Test
    @DisplayName("캠퍼스 공통 복합전공 불가 학과 ID를 반환한다")
    void findComplexRestrictedByFromCampus() {
        // when
        List<Long> result = departmentRestrictionRepository.findComplexRestrictedIdsByFromCampus(campus.getId());

        // then: Row B만 해당 (Row A는 플래그 불일치, Row C/D는 fromDepartment IS NOT NULL)
        assertThat(result).containsExactly(dept2.getId());
    }

    @Test
    @DisplayName("학과별 이중전공 불가 학과 ID를 반환한다")
    void findDoubleRestrictedByFromDepartment() {
        // when
        List<Long> result = departmentRestrictionRepository.findDoubleRestrictedIdsByFromDepartment(fromDept.getId());

        // then: Row D만 해당 (Row C는 플래그 불일치, Row A/B는 fromCampus IS NOT NULL)
        assertThat(result).containsExactly(dept3.getId());
    }

    @Test
    @DisplayName("학과별 복합전공 불가 학과 ID를 반환한다")
    void findComplexRestrictedByFromDepartment() {
        // when
        List<Long> result = departmentRestrictionRepository.findComplexRestrictedIdsByFromDepartment(fromDept.getId());

        // then: Row C만 해당 (Row D는 플래그 불일치, Row A/B는 fromCampus IS NOT NULL)
        assertThat(result).containsExactly(dept1.getId());
    }
}
