package com.campusnavi.backend.official.post.recommend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "feed_recommend_snapshot_item")
public class FeedRecommendSnapshotItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "snapshot_id", nullable = false)
    private FeedRecommendSnapshot snapshot;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    public static FeedRecommendSnapshotItem of(FeedRecommendSnapshot snapshot, Long postId, int sortOrder) {
        FeedRecommendSnapshotItem item = new FeedRecommendSnapshotItem();
        item.snapshot = snapshot;
        item.postId = postId;
        item.sortOrder = sortOrder;
        return item;
    }
}
