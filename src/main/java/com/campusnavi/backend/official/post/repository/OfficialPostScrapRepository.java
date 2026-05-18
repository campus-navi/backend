package com.campusnavi.backend.official.post.repository;

import com.campusnavi.backend.official.post.entity.OfficialPostScrap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OfficialPostScrapRepository extends JpaRepository<OfficialPostScrap, Long> {

    boolean existsByMemberIdAndPostId(Long memberId, Long postId);

    List<OfficialPostScrap> findByMemberIdAndPostId(Long memberId, Long postId);

    @Query("SELECT s.scrapFolderId FROM OfficialPostScrap s WHERE s.memberId = :memberId AND s.post.id = :postId")
    List<Long> findScrapFolderIdsByMemberIdAndPostId(@Param("memberId") Long memberId, @Param("postId") Long postId);

    @Query("SELECT COUNT(DISTINCT s.post.id) FROM OfficialPostScrap s WHERE s.memberId = :memberId")
    long countDistinctPostByMemberId(@Param("memberId") Long memberId);
}
