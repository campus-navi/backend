package com.campusnavi.backend.official.repository;

import com.campusnavi.backend.official.entity.OfficialPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Set;

public interface OfficialPostRepository extends JpaRepository<OfficialPost, Long> {
}
