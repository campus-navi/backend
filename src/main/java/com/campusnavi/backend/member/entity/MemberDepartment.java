package com.campusnavi.backend.member.entity;

import com.campusnavi.backend.university.entity.Department;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "member_department",
        uniqueConstraints = @UniqueConstraint(columnNames = {"member_id", "department_id"}, name = "uq_member_department"))
public class MemberDepartment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    static MemberDepartment create(Member member, Department department) {
        MemberDepartment memberDepartment = new MemberDepartment();
        memberDepartment.member = member;
        memberDepartment.department = department;
        return memberDepartment;
    }
}
