package com.campusnavi.backend.official.post.recommend.repository;

import com.campusnavi.backend.official.post.recommend.entity.FeedRecommendSnapshot;
import com.campusnavi.backend.official.post.recommend.entity.FeedRecommendSnapshotItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FeedRecommendSnapshotItemRepository extends JpaRepository<FeedRecommendSnapshotItem, Long> {

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM FeedRecommendSnapshotItem i WHERE i.snapshot = :snapshot")
    void deleteBySnapshot(@Param("snapshot") FeedRecommendSnapshot snapshot);
}
