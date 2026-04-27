package com.campusnavi.backend.member.repository;

import com.campusnavi.backend.member.entity.Member;
import com.campusnavi.backend.member.entity.MemberRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member,Long> {
    boolean existsByRoleAndUniversityId(MemberRole role, Long universityId);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByNickname(String nickname);
    Optional<Member> findByUsername(String username);

    @Override
    @Query("SELECT m FROM Member m WHERE m.id = :id AND m.status <> com.campusnavi.backend.member.entity.MemberStatus.WITHDRAWN")
    Optional<Member> findById(@Param("id") Long id);
}
