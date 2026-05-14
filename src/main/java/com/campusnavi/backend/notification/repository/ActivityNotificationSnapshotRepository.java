package com.campusnavi.backend.notification.repository;

import com.campusnavi.backend.notification.entity.ActivityNotificationSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Set;

public interface ActivityNotificationSnapshotRepository
        extends JpaRepository<ActivityNotificationSnapshot, Long> {

    @Query("SELECT s.memberId FROM ActivityNotificationSnapshot s " +
           "WHERE s.missedDate = :missedDate AND s.memberId IN :memberIds")
    Set<Long> findMemberIdsByMissedDateAndMemberIdIn(@Param("missedDate") LocalDate missedDate,
                                                    @Param("memberIds") Collection<Long> memberIds);

    @Modifying
    @Query("DELETE FROM ActivityNotificationSnapshot s WHERE s.missedDate < :before")
    void deleteOlderThan(@Param("before") LocalDate before);
}
