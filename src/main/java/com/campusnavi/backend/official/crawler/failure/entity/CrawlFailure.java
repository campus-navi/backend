package com.campusnavi.backend.official.crawler.failure.entity;

import com.campusnavi.backend.official.crawler.dto.PostList;
import com.campusnavi.backend.official.source.entity.OfficialSource;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"source_id", "original_id"}))
public class CrawlFailure {

    private static final int MAX_ERROR_LENGTH = 4000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long sourceId;

    @Column(nullable = false, length = 200)
    private String originalId;

    @Column(nullable = false, length = 500)
    private String detailUrl;

    @Column(length = 500)
    private String title;

    @Column(length = 50)
    private String publisher;

    private LocalDate publishedAt;

    @Column(nullable = false)
    private int retryCount = 0;

    @Column(length = MAX_ERROR_LENGTH)
    private String lastError;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime lastRetriedAt;

    public static CrawlFailure create(OfficialSource source, PostList post, Throwable e) {
        CrawlFailure failure = new CrawlFailure();
        failure.sourceId = source.getId();
        failure.originalId = post.originalId();
        failure.detailUrl = post.detailUrl();
        failure.title = post.title();
        failure.publisher = post.publisher();
        failure.publishedAt = post.publishedAt();
        failure.lastError = formatError(e);
        return failure;
    }

    /**
     * 본 크롤링에서 같은 게시글이 다시 실패했을 때 호출. retryCount 는 건드리지 않고
     * 마지막 오류 메시지/시각만 갱신한다 — retryCount 의 의미를 "재시도 시도 실패 수"로 한정.
     */
    public void touchError(Throwable e) {
        this.lastError = formatError(e);
        this.lastRetriedAt = LocalDateTime.now();
    }

    /**
     * 재시도 스케줄에서 실패했을 때 호출. retryCount 를 +1 하고 오류 메타를 갱신한다.
     */
    public void incrementRetry(Throwable e) {
        this.retryCount++;
        this.lastError = formatError(e);
        this.lastRetriedAt = LocalDateTime.now();
    }

    public PostList toPostList() {
        return new PostList(originalId, title, publisher, detailUrl, publishedAt);
    }

    private static String formatError(Throwable e) {
        if (e == null) return null;
        String message = e.getClass().getName() + ": " + (e.getMessage() == null ? "" : e.getMessage());
        return message.length() > MAX_ERROR_LENGTH ? message.substring(0, MAX_ERROR_LENGTH) : message;
    }
}
