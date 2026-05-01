package com.campusnavi.backend.official.post.repository;

import com.campusnavi.backend.official.post.entity.OfficialAttachmentDownload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface OfficialAttachmentDownloadRepository extends JpaRepository<OfficialAttachmentDownload, Long> {

    @Query("SELECT d.attachmentId FROM OfficialAttachmentDownload d " +
            "WHERE d.memberId = :memberId AND d.attachmentId IN :attachmentIds")
    Set<Long> findDownloadedAttachmentIds(@Param("memberId") Long memberId,
                                          @Param("attachmentIds") List<Long> attachmentIds);
}
