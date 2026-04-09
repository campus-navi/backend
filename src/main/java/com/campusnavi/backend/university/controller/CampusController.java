package com.campusnavi.backend.university.controller;

import com.campusnavi.backend.global.response.ApiResponse;
import com.campusnavi.backend.university.dto.CampusSummaryResponse;
import com.campusnavi.backend.university.dto.DepartmentSummaryResponse;
import com.campusnavi.backend.university.service.CampusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/campuses")
public class CampusController {

    private final CampusService campusService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CampusSummaryResponse>>> getCampusList() {
        return ResponseEntity.ok(ApiResponse.ok(campusService.getCampusList()));
    }

    @GetMapping("/{campusId}/departments")
    public ResponseEntity<ApiResponse<List<DepartmentSummaryResponse>>> getDepartmentList(@PathVariable Long campusId) {
        return ResponseEntity.ok(ApiResponse.ok(campusService.getDepartmentList(campusId)));
    }
}
