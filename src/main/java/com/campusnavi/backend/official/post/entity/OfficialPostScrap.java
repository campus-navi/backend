package com.campusnavi.backend.official.post.entity;

import com.campusnavi.backend.global.common.BaseCreatedAtEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"member_id", "post_id"}))
public class OfficialPostScrap extends BaseCreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private OfficialPost post;

    public static OfficialPostScrap create(Long memberId, OfficialPost post) {
        OfficialPostScrap scrap = new OfficialPostScrap();
        scrap.memberId = memberId;
        scrap.post = post;
        return scrap;
    }
}
