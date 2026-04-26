package com.campusnavi.backend.member.entity;

import com.campusnavi.backend.tag.entity.Tag;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "member_interest",
        uniqueConstraints = @UniqueConstraint(columnNames = {"member_id", "tag_id"}, name = "uq_member_interest"))
public class MemberInterest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long memberId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    public static MemberInterest create(Long memberId, Tag tag) {
        MemberInterest memberInterest = new MemberInterest();
        memberInterest.memberId = memberId;
        memberInterest.tag = tag;
        return memberInterest;
    }
}
