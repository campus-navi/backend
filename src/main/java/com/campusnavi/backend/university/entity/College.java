package com.campusnavi.backend.university.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "college", uniqueConstraints = @UniqueConstraint(
        columnNames = {"campus_id", "name"}, name = "uq_campus_college_name"))
public class College {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campus_id", nullable = false)
    private Campus campus;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String code;

    public static College create(Campus campus, String name, String code) {
        College college = new College();
        college.campus = campus;
        college.name = name;
        college.code = code;
        return college;
    }
}
