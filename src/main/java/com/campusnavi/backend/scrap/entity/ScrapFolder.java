package com.campusnavi.backend.scrap.entity;

import com.campusnavi.backend.global.common.BaseCreatedAtEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "scrap_folder",
        uniqueConstraints = @UniqueConstraint(name = "uq_scrap_folder_member_name", columnNames = {"member_id", "name"}))
public class ScrapFolder extends BaseCreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(nullable = false, length = 20)
    private String name;

    @Column(length = 20)
    private String description;

    public static ScrapFolder create(Long memberId, String name, String description) {
        ScrapFolder folder = new ScrapFolder();
        folder.memberId = memberId;
        folder.name = name;
        folder.description = description;
        return folder;
    }

    public void update(String name, String description) {
        this.name = name;
        this.description = description;
    }
}
