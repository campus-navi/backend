package com.campusnavi.backend.member.repository;

import com.campusnavi.backend.member.entity.MemberInterest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface MemberInterestRepository extends JpaRepository<MemberInterest,Long> {
    @Modifying
    @Query("DELETE FROM MemberInterest mi WHERE mi.memberId = :memberId")
    void deleteAllByMemberId(Long memberId);
}
