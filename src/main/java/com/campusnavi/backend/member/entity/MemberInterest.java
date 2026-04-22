package com.campusnavi.backend.member.entity;

import com.campusnavi.backend.interest.entity.InterestTag;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "member_interest",
        uniqueConstraints = @UniqueConstraint(columnNames = {"member_id", "interest_tag_id"}, name = "uq_member_interest"))
public class MemberInterest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long memberId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interest_tag_id", nullable = false)
    private InterestTag interestTag;

    public static MemberInterest create(Long memberId, InterestTag interestTag) {
        MemberInterest memberInterest = new MemberInterest();
        memberInterest.memberId = memberId;
        memberInterest.interestTag = interestTag;
        return memberInterest;
    }
}
