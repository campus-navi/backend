package com.campusnavi.backend.studio.academicplan.service;

import com.campusnavi.backend.studio.academicplan.entity.MajorType;
import com.campusnavi.backend.studio.academicplan.controller.dto.TargetCampusResponse;
import com.campusnavi.backend.studio.academicplan.controller.dto.TargetDepartmentResponse;
import com.campusnavi.backend.studio.academicplan.controller.dto.TargetMajorResponse;
import com.campusnavi.backend.studio.academicplan.repository.DepartmentRestrictionRepository;
import com.campusnavi.backend.studio.academicplan.repository.TargetMajorRepository;
import com.campusnavi.backend.global.exception.BusinessException;
import com.campusnavi.backend.global.exception.ErrorCode;
import com.campusnavi.backend.member.entity.Member;
import com.campusnavi.backend.member.entity.MemberDepartment;
import com.campusnavi.backend.member.repository.MemberRepository;
import com.campusnavi.backend.university.repository.CampusRepository;
import com.campusnavi.backend.university.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AcademicPlanTargetService {

    private final MemberRepository memberRepository;
    private final CampusRepository campusRepository;
    private final DepartmentRepository departmentRepository;
    private final TargetMajorRepository targetMajorRepository;
    private final DepartmentRestrictionRepository departmentRestrictionRepository;

    public List<TargetCampusResponse> getCampuses(Long universityId) {
        return campusRepository.findByUniversityIdOrderByNameAsc(universityId)
                .stream()
                .map(TargetCampusResponse::from)
                .toList();
    }

    public List<TargetDepartmentResponse> getDepartments(Long campusId, MajorType type, Long memberId) {
        Member member = memberRepository.findProfileById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        Set<Long> restrictedIds = new HashSet<>(
                restrictedIdsByFromCampus(member.getCampus().getId(), type)
        );
        for (MemberDepartment md : member.getMemberDepartments()) {
            restrictedIds.addAll(restrictedIdsByFromDepartment(md.getDepartment().getId(), type));
        }

        return departmentRepository.findByCampusIdOrderByNameAsc(campusId)
                .stream()
                .filter(d -> !restrictedIds.contains(d.getId()))
                .map(TargetDepartmentResponse::from)
                .toList();
    }

    public List<TargetMajorResponse> getMajors(Long campusId, MajorType type) {
        if (type != MajorType.CONVERGENCE_MAJOR && type != MajorType.STUDENT_DESIGN) {
            throw new BusinessException(ErrorCode.INVALID_PARAM);
        }
        return targetMajorRepository.findByCampusIdAndMajorTypeOrderByNameAsc(campusId, type)
                .stream()
                .map(TargetMajorResponse::from)
                .toList();
    }

    private List<Long> restrictedIdsByFromCampus(Long campusId, MajorType type) {
        return switch (type) {
            case DOUBLE_MAJOR  -> departmentRestrictionRepository.findDoubleRestrictedIdsByFromCampus(campusId);
            case COMPLEX_MAJOR -> departmentRestrictionRepository.findComplexRestrictedIdsByFromCampus(campusId);
            default -> throw new BusinessException(ErrorCode.INVALID_PARAM);
        };
    }

    private List<Long> restrictedIdsByFromDepartment(Long departmentId, MajorType type) {
        return switch (type) {
            case DOUBLE_MAJOR  -> departmentRestrictionRepository.findDoubleRestrictedIdsByFromDepartment(departmentId);
            case COMPLEX_MAJOR -> departmentRestrictionRepository.findComplexRestrictedIdsByFromDepartment(departmentId);
            default -> throw new BusinessException(ErrorCode.INVALID_PARAM);
        };
    }
}
