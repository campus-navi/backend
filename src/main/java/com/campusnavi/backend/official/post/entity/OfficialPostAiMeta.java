package com.campusnavi.backend.official.post.entity;

import com.campusnavi.backend.global.common.BaseEntity;
import com.campusnavi.backend.global.common.ProcessingStatus;
import com.campusnavi.backend.tag.entity.Tag;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OfficialPostAiMeta extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "official_post_id", nullable = false, unique = true)
    private OfficialPost officialPost;

    private String summary;

    private Integer targetGradeMin;

    private Integer targetGradeMax;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id")
    private Tag tag;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "TEXT[]")
    private List<String> keyword;

    @Column(length = 255)
    private String contactPhone;

    @Column(length = 200)
    private String contactEmail;

    private LocalDate startDate;

    private LocalTime startTime;

    private LocalDate endDate;

    private LocalTime endTime;

    @Column(length = 500)
    private String requiredDocuments;

    @Enumerated(EnumType.STRING)
    @Column(name = "apply_method_type", length = 20)
    private ApplyMethodType applyMethodType;

    @Column(name = "apply_method_detail", columnDefinition = "TEXT")
    private String applyMethodDetail;

    @Column(columnDefinition = "TEXT")
    private String eligibility;

    @Column(nullable = false)
    private boolean isApplicable;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProcessingStatus status;

    @Column(nullable = false)
    private int retryCount;

    private String failureReason;

    private LocalDateTime processedAt;

    public static OfficialPostAiMeta pending(OfficialPost post) {
        OfficialPostAiMeta meta = new OfficialPostAiMeta();
        meta.officialPost = post;
        meta.status = ProcessingStatus.PENDING;
        meta.retryCount = 0;
        return meta;
    }

    public void processCompleted(
            String summary,
            Integer targetGradeMin,
            Integer targetGradeMax,
            Tag tag,
            List<String> keyword,
            String contactPhone,
            String contactEmail,
            LocalDate startDate,
            LocalTime startTime,
            LocalDate endDate,
            LocalTime endTime,
            String requiredDocuments,
            ApplyMethodType applyMethodType,
            String applyMethodDetail,
            String eligibility,
            boolean isApplicable
    ) {
        this.summary = summary;
        this.targetGradeMin = targetGradeMin;
        this.targetGradeMax = targetGradeMax;
        this.tag = tag;
        this.keyword = keyword;
        this.contactPhone = contactPhone;
        this.contactEmail = contactEmail;
        this.startDate = startDate;
        this.startTime = startTime;
        this.endDate = endDate;
        this.endTime = endTime;
        this.requiredDocuments = requiredDocuments;
        this.applyMethodType = applyMethodType;
        this.applyMethodDetail = applyMethodDetail;
        this.eligibility = eligibility;
        this.isApplicable = isApplicable;
        this.status = ProcessingStatus.DONE;
        this.processedAt = LocalDateTime.now();
        this.failureReason = null;
    }

    public void processFailed(String reason) {
        this.status = ProcessingStatus.FAILED;
        this.retryCount++;
        this.failureReason = reason;
    }
}
