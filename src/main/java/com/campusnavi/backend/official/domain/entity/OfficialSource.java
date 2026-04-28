package com.campusnavi.backend.official.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OfficialSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long universityId;

    private Long campusId;

    private Long collegeId;

    private Long departmentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SourceType sourceType;

    @Column(nullable = false, length = 50)
    private String parserType;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 500)
    private String listUrl;

    @Column(nullable = false)
    private LocalDate lastCrawledAt = LocalDate.now();

    @Column(nullable = false)
    private boolean isActive = true;

    public static OfficialSource create(Long universityId, Long campusId, Long collegeId,
                                        Long departmentId, SourceType sourceType,
                                        String parserType, String name, String listUrl) {
        OfficialSource source = new OfficialSource();
        source.universityId = universityId;
        source.campusId = campusId;
        source.collegeId = collegeId;
        source.departmentId = departmentId;
        source.sourceType = sourceType;
        source.parserType = parserType;
        source.name = name;
        source.listUrl = listUrl;
        return source;
    }

    public void updateLastCrawledAt(LocalDate crawledAt) {
        this.lastCrawledAt = crawledAt;
    }

    public void deactivate() {
        this.isActive = false;
    }
}