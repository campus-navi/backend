package com.campusnavi.backend.official.entity;

import com.campusnavi.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"source_id", "original_id"}))
public class OfficialPost extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id", nullable = false)
    private OfficialSource source;

    private Long universityId;

    private Long campusId;

    private Long collegeId;

    private Long departmentId;

    @Column(nullable = false, length = 200)
    private String originalId;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false, length = 50)
    private String publisher;

    @Column(columnDefinition = "TEXT")
    private String plainText;

    @Column(columnDefinition = "TEXT")
    private String htmlContent;

    @Column(nullable = false, length = 500)
    private String sourceUrl;

    private LocalDate publishedAt;

    @Column(nullable = false)
    private LocalDateTime crawledAt;

    @Column(nullable = false)
    private boolean isActive = true;

    public static OfficialPost create(OfficialSource source, String originalId, String title,
                                      String publisher, String plainText, String htmlContent,
                                      String sourceUrl, LocalDate publishedAt, LocalDateTime crawledAt) {
        OfficialPost post = new OfficialPost();
        post.source = source;
        post.universityId = source.getUniversityId();
        post.campusId = source.getCampusId();
        post.collegeId = source.getCollegeId();
        post.departmentId = source.getDepartmentId();
        post.originalId = originalId;
        post.title = title;
        post.publisher = publisher;
        post.plainText = plainText;
        post.htmlContent = htmlContent;
        post.sourceUrl = sourceUrl;
        post.publishedAt = publishedAt;
        post.crawledAt = crawledAt;
        return post;
    }

    public void deactivate() {
        this.isActive = false;
    }
}