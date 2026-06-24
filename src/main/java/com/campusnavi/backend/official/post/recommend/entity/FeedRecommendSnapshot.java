package com.campusnavi.backend.official.post.recommend.entity;

import com.campusnavi.backend.global.common.BaseCreatedAtEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "feed_recommend_snapshot",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_feed_recommend_snapshot_member_slot",
                columnNames = {"member_id", "slot_at"}))
public class FeedRecommendSnapshot extends BaseCreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "slot_at", nullable = false)
    private LocalDateTime slotAt;

    @OneToMany(mappedBy = "snapshot", fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC")
    private List<FeedRecommendSnapshotItem> items = new ArrayList<>();

    public List<Long> getPostIds() {
        return items.stream()
                .map(FeedRecommendSnapshotItem::getPostId)
                .toList();
    }
}
