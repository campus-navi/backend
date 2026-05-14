package com.campusnavi.backend.official.post.recommend.entity;

import com.campusnavi.backend.global.common.BaseCreatedAtEntity;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
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

    @Type(JsonBinaryType.class)
    @Column(name = "post_ids", nullable = false, columnDefinition = "jsonb")
    private List<Long> postIds;
}
