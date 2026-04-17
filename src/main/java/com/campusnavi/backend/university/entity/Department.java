package com.campusnavi.backend.university.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "department", uniqueConstraints = @UniqueConstraint(columnNames = {"campus_id", "name"}, name = "uq_campus_department_name"))
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campus_id", nullable = false)
    private Campus campus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "college_id")
    private College college;

    @Column(nullable = false)
    private String name;

    public static Department create(Campus campus, String name) {
        Department department = new Department();
        department.campus = campus;
        department.name = name;
        return department;
    }

}
