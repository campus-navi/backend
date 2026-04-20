package com.campusnavi.backend.official.repository;

import com.campusnavi.backend.official.entity.OfficialPost;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfficialPostRepository extends JpaRepository<OfficialPost,Long> {
}
