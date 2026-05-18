package com.campusnavi.backend.member.repository;

import com.campusnavi.backend.member.entity.MemberInterest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MemberInterestRepository extends JpaRepository<MemberInterest,Long> {
    @Modifying
    @Query("DELETE FROM MemberInterest mi WHERE mi.memberId = :memberId")
    void deleteAllByMemberId(Long memberId);

    @Query("SELECT mi.tag.id FROM MemberInterest mi WHERE mi.memberId = :memberId")
    List<Long> findTagIdsByMemberId(@Param("memberId") Long memberId);

    boolean existsByMemberId(Long memberId);

    long countByMemberId(Long memberId);
}
