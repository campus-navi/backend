package com.campusnavi.backend.university.service;

import com.campusnavi.backend.global.exception.BusinessException;
import com.campusnavi.backend.global.exception.ErrorCode;
import com.campusnavi.backend.university.dto.CampusSummaryResponse;
import com.campusnavi.backend.university.dto.DepartmentSummaryResponse;
import com.campusnavi.backend.university.repository.CampusRepository;
import com.campusnavi.backend.university.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CampusService {

    private final CampusRepository campusRepository;
    private final DepartmentRepository departmentRepository;

    public List<CampusSummaryResponse> getCampusList() {
        return campusRepository.findAllByOrderByNameAsc()
                .stream()
                .map(CampusSummaryResponse::of)
                .toList();
    }

    public List<DepartmentSummaryResponse> getDepartmentList(Long campusId){
        if (!campusRepository.existsById(campusId)){
            throw new BusinessException(ErrorCode.CAMPUS_NOT_FOUND);
        }
        return departmentRepository
                .findByCampusIdOrderByNameAsc(campusId)
                .stream()
                .map(DepartmentSummaryResponse::of)
                .toList();
    }
}
