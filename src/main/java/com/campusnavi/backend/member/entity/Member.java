package com.campusnavi.backend.member.entity;

import com.campusnavi.backend.global.common.BaseEntity;
import com.campusnavi.backend.university.entity.Campus;
import com.campusnavi.backend.university.entity.Department;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false,unique = true)
    private String email;

    @Column(nullable = false, length = 30,unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String nickname;

    @Column(length = 30)
    private String name;

    @Column(length = 10)
    private String studentNumber;

    @Column(nullable = false)
    private Long universityId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campus_id")
    private Campus campus;

    @Column
    private Integer admissionYear;

    @Column
    private Integer grade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberStatus status;

    @OneToMany(mappedBy = "member", cascade = CascadeType.PERSIST)
    private List<MemberDepartment> memberDepartments = new ArrayList<>();

    private LocalDateTime deletedAt;

    public static Member join(String email, String username, String password, String nickname,
                              String name, String studentNumber, Long universityId, Campus campus,
                              Integer admissionYear, Integer grade) {
        Member member = new Member();
        member.email = email;
        member.username = username;
        member.password = password;
        member.nickname = nickname;
        member.name = name;
        member.studentNumber = studentNumber;
        member.universityId = universityId;
        member.campus = campus;
        member.admissionYear = admissionYear;
        member.grade = grade;
        member.role = MemberRole.USER;
        member.status = MemberStatus.ACTIVE;
        return member;
    }

    public static Member join(String email, String username, String password, String nickname,
                              Long universityId, Campus campus, Integer admissionYear, Integer grade) {
        return join(email, username, password, nickname, null, null, universityId, campus, admissionYear, grade);
    }

    public static Member createAdmin(String email, String username, String password,
                                     String nickname, Long universityId) {
        Member member = new Member();
        member.email = email;
        member.username = username;
        member.password = password;
        member.nickname = nickname;
        member.universityId = universityId;
        member.role = MemberRole.ADMIN;
        member.status = MemberStatus.ACTIVE;
        return member;
    }

    public void addDepartment(Department department){
        MemberDepartment memberDepartment = MemberDepartment.create(this,department);
        memberDepartments.add(memberDepartment);
    }

    public void withdraw(){
        String withdrawn = "[탈퇴된 회원" + id +"]";
        this.username = withdrawn;
        this.email = withdrawn + "@withdrawn";
        this.nickname = withdrawn;
        this.name = withdrawn;
        this.studentNumber = null;
        this.password = UUID.randomUUID().toString();
        this.status = MemberStatus.WITHDRAWN;
        this.deletedAt = LocalDateTime.now();
    }

    public void changeUsername(String username) {
        this.username = username;
    }

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void changeStudentNumber(Integer admissionYear, String studentNumber) {
        this.admissionYear = admissionYear;
        this.studentNumber = studentNumber;
    }

    public void changeGrade(Integer grade) {
        this.grade = grade;
    }
}
