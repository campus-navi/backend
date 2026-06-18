package com.campusnavi.backend.studio.academicplan.entity;

import com.campusnavi.backend.university.entity.Campus;
import com.campusnavi.backend.university.entity.Department;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "department_restriction")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class DepartmentRestriction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_campus_id")
    private Campus fromCampus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_department_id")
    private Department fromDepartment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_department_id", nullable = false)
    private Department toDepartment;

    @Column(nullable = false)
    private boolean restrictDoubleMajor;

    @Column(nullable = false)
    private boolean restrictComplexMajor;

    public static DepartmentRestriction ofCampus(Campus fromCampus, Department toDepartment,
                                                  boolean restrictDoubleMajor, boolean restrictComplexMajor) {
        DepartmentRestriction deptRestriction = new DepartmentRestriction();
        deptRestriction.fromCampus = fromCampus;
        deptRestriction.toDepartment = toDepartment;
        deptRestriction.restrictDoubleMajor = restrictDoubleMajor;
        deptRestriction.restrictComplexMajor = restrictComplexMajor;
        return deptRestriction;
    }

    public static DepartmentRestriction ofDepartment(Department fromDepartment, Department toDepartment,
                                                      boolean restrictDoubleMajor, boolean restrictComplexMajor) {
        DepartmentRestriction deptRestriction = new DepartmentRestriction();
        deptRestriction.fromDepartment = fromDepartment;
        deptRestriction.toDepartment = toDepartment;
        deptRestriction.restrictDoubleMajor = restrictDoubleMajor;
        deptRestriction.restrictComplexMajor = restrictComplexMajor;
        return deptRestriction;
    }
}
