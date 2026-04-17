package com.campusnavi.backend.university.repository;

import com.campusnavi.backend.university.entity.Campus;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CampusRepository extends JpaRepository<Campus,Long> {
    List<Campus> findAllByOrderByNameAsc();
    boolean existsById(@NonNull Long id);
}
