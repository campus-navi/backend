package com.campusnavi.backend.official.post.repository;

import com.campusnavi.backend.official.post.dto.FolderScrapResponse;
import com.campusnavi.backend.official.post.dto.RecentScrapResponse;
import com.campusnavi.backend.official.post.entity.OfficialPostScrap;
import org.springframework.data.domain.Pageable;
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

    @Query("SELECT new com.campusnavi.backend.official.post.dto.FolderScrapResponse("
            + "s.id, p.id, p.title, t.name, m.endDate, p.publishedAt, p.isActive) "
            + "FROM OfficialPostScrap s JOIN s.post p "
            + "LEFT JOIN OfficialPostAiMeta m ON m.officialPost = p "
            + "LEFT JOIN m.tag t "
            + "WHERE s.memberId = :memberId AND s.scrapFolderId = :folderId "
            + "ORDER BY s.createdAt DESC")
    List<FolderScrapResponse> findFolderScraps(@Param("memberId") Long memberId,
                                               @Param("folderId") Long folderId);

    @Query("SELECT s.post.id FROM OfficialPostScrap s WHERE s.memberId = :memberId "
            + "GROUP BY s.post.id ORDER BY MAX(s.createdAt) DESC")
    List<Long> findRecentScrappedPostIds(@Param("memberId") Long memberId, Pageable pageable);

    @Query("SELECT new com.campusnavi.backend.official.post.dto.RecentScrapResponse("
            + "p.id, p.title, t.name, m.endDate, p.publishedAt) "
            + "FROM OfficialPost p "
            + "LEFT JOIN OfficialPostAiMeta m ON m.officialPost = p "
            + "LEFT JOIN m.tag t "
            + "WHERE p.id IN :postIds")
    List<RecentScrapResponse> findRecentScrapCards(@Param("postIds") List<Long> postIds);
}
