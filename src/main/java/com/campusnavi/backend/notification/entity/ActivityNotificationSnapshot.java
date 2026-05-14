package com.campusnavi.backend.notification.entity;

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

import java.time.LocalDate;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "activity_notification_snapshot",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_activity_notification_snapshot_member_date",
                columnNames = {"member_id", "missed_date"}))
public class ActivityNotificationSnapshot extends BaseCreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "missed_date", nullable = false)
    private LocalDate missedDate;

    @Type(JsonBinaryType.class)
    @Column(name = "post_ids", nullable = false, columnDefinition = "jsonb")
    private List<Long> postIds;

    public static ActivityNotificationSnapshot of(Long memberId, LocalDate missedDate, List<Long> postIds) {
        ActivityNotificationSnapshot snapshot = new ActivityNotificationSnapshot();
        snapshot.memberId = memberId;
        snapshot.missedDate = missedDate;
        snapshot.postIds = postIds;
        return snapshot;
    }
}
