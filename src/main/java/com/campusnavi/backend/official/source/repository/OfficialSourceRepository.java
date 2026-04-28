package com.campusnavi.backend.official.source.repository;

import com.campusnavi.backend.official.source.entity.OfficialSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OfficialSourceRepository extends JpaRepository<OfficialSource,Long> {
    List<OfficialSource> findAllByIsActiveTrue();
}
