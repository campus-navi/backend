package com.campusnavi.backend.university.service;

import com.campusnavi.backend.global.exception.BusinessException;
import com.campusnavi.backend.global.exception.ErrorCode;
import com.campusnavi.backend.university.dto.CampusSummaryResponse;
import com.campusnavi.backend.university.dto.DepartmentSummaryResponse;
import com.campusnavi.backend.university.entity.Campus;
import com.campusnavi.backend.university.entity.Department;
import com.campusnavi.backend.university.entity.University;
import com.campusnavi.backend.university.repository.CampusRepository;
import com.campusnavi.backend.university.repository.DepartmentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CampusServiceTest {

    @Mock
    private CampusRepository campusRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @InjectMocks
    private CampusService campusService;

    @Test
    @DisplayName("캠퍼스가 존재하면 캠퍼스 DTO목록 반환")
    void getCampusListSuccess() {
        //given
        University university = University.create("테스트대학교");
        Campus campus = Campus.create(university, "서울캠퍼스", "TEST_SEOUL", "test.ac.kr");
        given(campusRepository.findAllByOrderByNameAsc()).willReturn(List.of(campus));

        //when
        List<CampusSummaryResponse> result = campusService.getCampusList();

        //then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().name()).isEqualTo("서울캠퍼스");
    }

    @Test
    @DisplayName("유효한 캠퍼스 ID를 받으면 학과 DTO 목록 반환")
    void getDepartmentListSuccess() {
        //given
        Long campusId = 1L;
        University university = University.create("테스트대학교");
        Campus campus = Campus.create(university, "서울캠퍼스", "TEST_SEOUL", "test.ac.kr");
        Department dept = Department.create(campus, "컴퓨터공학과");
        given(campusRepository.existsById(campusId)).willReturn(true);
        given(departmentRepository.findByCampusIdOrderByNameAsc(campusId)).willReturn(List.of(dept));

        //when
        List<DepartmentSummaryResponse> result = campusService.getDepartmentList(campusId);

        //then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().name()).isEqualTo("컴퓨터공학과");
    }

    @Test
    @DisplayName("존재하지 않는 캠퍼스 ID를 받으면 BusinessException발생")
    void getDepartmentListNotFoundByCampusId() {
        //given
        Long campusId = 99L;
        given(campusRepository.existsById(campusId)).willReturn(false);

        //when, then
        assertThatThrownBy(() -> campusService.getDepartmentList(campusId))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.CAMPUS_NOT_FOUND));
    }
}
