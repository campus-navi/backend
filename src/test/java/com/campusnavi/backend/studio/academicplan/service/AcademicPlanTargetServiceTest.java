package com.campusnavi.backend.studio.academicplan.service;

import com.campusnavi.backend.studio.academicplan.entity.MajorType;
import com.campusnavi.backend.studio.academicplan.controller.dto.TargetCampusResponse;
import com.campusnavi.backend.studio.academicplan.controller.dto.TargetDepartmentResponse;
import com.campusnavi.backend.studio.academicplan.controller.dto.TargetMajorResponse;
import com.campusnavi.backend.studio.academicplan.entity.TargetMajor;
import com.campusnavi.backend.studio.academicplan.repository.DepartmentRestrictionRepository;
import com.campusnavi.backend.studio.academicplan.repository.TargetMajorRepository;
import com.campusnavi.backend.global.exception.BusinessException;
import com.campusnavi.backend.global.exception.ErrorCode;
import com.campusnavi.backend.member.entity.Member;
import com.campusnavi.backend.member.entity.MemberDepartment;
import com.campusnavi.backend.member.repository.MemberRepository;
import com.campusnavi.backend.university.entity.Campus;
import com.campusnavi.backend.university.entity.Department;
import com.campusnavi.backend.university.entity.University;
import com.campusnavi.backend.university.repository.CampusRepository;
import com.campusnavi.backend.university.repository.DepartmentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class AcademicPlanTargetServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private CampusRepository campusRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private TargetMajorRepository targetMajorRepository;

    @Mock
    private DepartmentRestrictionRepository departmentRestrictionRepository;

    @InjectMocks
    private AcademicPlanTargetService academicPlanTargetService;

    private static final Long MEMBER_ID = 1L;
    private static final Long UNIVERSITY_ID = 10L;
    private static final Long MY_CAMPUS_ID = 1L;
    private static final Long TARGET_CAMPUS_ID = 2L;
    private static final Long MY_DEPARTMENT_ID = 11L;

    @Nested
    @DisplayName("캠퍼스 목록 조회")
    class GetCampuses {

        @Test
        @DisplayName("소속 대학교 기준 캠퍼스 목록을 반환한다")
        void success() {
            // given
            Campus campus1 = mock(Campus.class);
            Campus campus2 = mock(Campus.class);
            given(campus1.getId()).willReturn(1L);
            given(campus1.getName()).willReturn("서울캠퍼스");
            given(campus2.getId()).willReturn(2L);
            given(campus2.getName()).willReturn("세종캠퍼스");
            given(campusRepository.findByUniversityIdOrderByNameAsc(UNIVERSITY_ID))
                    .willReturn(List.of(campus1, campus2));

            // when
            List<TargetCampusResponse> result = academicPlanTargetService.getCampuses(UNIVERSITY_ID);

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).name()).isEqualTo("서울캠퍼스");
        }
    }

    @Nested
    @DisplayName("대상 학과 목록 조회")
    class GetDepartments {

        @Test
        @DisplayName("멤버가 없으면 MEMBER_NOT_FOUND 예외를 던진다")
        void memberNotFound() {
            // given
            given(memberRepository.findProfileById(MEMBER_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() ->
                    academicPlanTargetService.getDepartments(TARGET_CAMPUS_ID, MajorType.DOUBLE_MAJOR, MEMBER_ID))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.MEMBER_NOT_FOUND));
        }

        @Test
        @DisplayName("캠퍼스 공통 불가 학과가 제외된 목록을 반환한다")
        void campusLevelFilter() {
            // given
            Campus myCampus = mock(Campus.class);
            given(myCampus.getId()).willReturn(MY_CAMPUS_ID);

            Member member = mock(Member.class);
            given(member.getCampus()).willReturn(myCampus);
            given(member.getMemberDepartments()).willReturn(List.of());
            given(memberRepository.findProfileById(MEMBER_ID)).willReturn(Optional.of(member));

            Department allowed = mock(Department.class);
            Department restricted = mock(Department.class);
            given(allowed.getId()).willReturn(20L);
            given(allowed.getName()).willReturn("경제통계학부");
            given(restricted.getId()).willReturn(21L);

            given(departmentRestrictionRepository.findDoubleRestrictedIdsByFromCampus(MY_CAMPUS_ID))
                    .willReturn(List.of(21L));
            given(departmentRepository.findByCampusIdOrderByNameAsc(TARGET_CAMPUS_ID))
                    .willReturn(List.of(allowed, restricted));

            // when
            List<TargetDepartmentResponse> result =
                    academicPlanTargetService.getDepartments(TARGET_CAMPUS_ID, MajorType.DOUBLE_MAJOR, MEMBER_ID);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).name()).isEqualTo("경제통계학부");
        }

        @Test
        @DisplayName("학과별 추가 불가 학과가 제외된 목록을 반환한다")
        void departmentLevelFilter() {
            // given
            Campus myCampus = mock(Campus.class);
            given(myCampus.getId()).willReturn(MY_CAMPUS_ID);

            Department myDept = mock(Department.class);
            given(myDept.getId()).willReturn(MY_DEPARTMENT_ID);

            MemberDepartment md = mock(MemberDepartment.class);
            given(md.getDepartment()).willReturn(myDept);

            Member member = mock(Member.class);
            given(member.getCampus()).willReturn(myCampus);
            given(member.getMemberDepartments()).willReturn(List.of(md));
            given(memberRepository.findProfileById(MEMBER_ID)).willReturn(Optional.of(member));

            Department allowed = mock(Department.class);
            Department restricted = mock(Department.class);
            given(allowed.getId()).willReturn(30L);
            given(allowed.getName()).willReturn("컴퓨터소프트웨어학과");
            given(restricted.getId()).willReturn(31L);

            given(departmentRestrictionRepository.findComplexRestrictedIdsByFromCampus(MY_CAMPUS_ID))
                    .willReturn(List.of());
            given(departmentRestrictionRepository.findComplexRestrictedIdsByFromDepartment(MY_DEPARTMENT_ID))
                    .willReturn(List.of(31L));
            given(departmentRepository.findByCampusIdOrderByNameAsc(TARGET_CAMPUS_ID))
                    .willReturn(List.of(allowed, restricted));

            // when
            List<TargetDepartmentResponse> result =
                    academicPlanTargetService.getDepartments(TARGET_CAMPUS_ID, MajorType.COMPLEX_MAJOR, MEMBER_ID);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).name()).isEqualTo("컴퓨터소프트웨어학과");
        }

        @Test
        @DisplayName("캠퍼스 불가와 학과 불가가 합산되어 제외된 목록을 반환한다")
        void combinedFilter() {
            // given
            Campus myCampus = mock(Campus.class);
            given(myCampus.getId()).willReturn(MY_CAMPUS_ID);

            Department myDept = mock(Department.class);
            given(myDept.getId()).willReturn(MY_DEPARTMENT_ID);

            MemberDepartment md = mock(MemberDepartment.class);
            given(md.getDepartment()).willReturn(myDept);

            Member member = mock(Member.class);
            given(member.getCampus()).willReturn(myCampus);
            given(member.getMemberDepartments()).willReturn(List.of(md));
            given(memberRepository.findProfileById(MEMBER_ID)).willReturn(Optional.of(member));

            Department allowed = mock(Department.class);
            Department restrictedByCampus = mock(Department.class);
            Department restrictedByDept = mock(Department.class);
            given(allowed.getId()).willReturn(40L);
            given(allowed.getName()).willReturn("경제통계학부");
            given(restrictedByCampus.getId()).willReturn(41L);
            given(restrictedByDept.getId()).willReturn(42L);

            given(departmentRestrictionRepository.findDoubleRestrictedIdsByFromCampus(MY_CAMPUS_ID))
                    .willReturn(List.of(41L));
            given(departmentRestrictionRepository.findDoubleRestrictedIdsByFromDepartment(MY_DEPARTMENT_ID))
                    .willReturn(List.of(42L));
            given(departmentRepository.findByCampusIdOrderByNameAsc(TARGET_CAMPUS_ID))
                    .willReturn(List.of(allowed, restrictedByCampus, restrictedByDept));

            // when
            List<TargetDepartmentResponse> result =
                    academicPlanTargetService.getDepartments(TARGET_CAMPUS_ID, MajorType.DOUBLE_MAJOR, MEMBER_ID);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).name()).isEqualTo("경제통계학부");
        }

        @Test
        @DisplayName("CONVERGENCE_MAJOR 전달 시 INVALID_PARAM 예외를 던진다")
        void invalidType() {
            // given
            Member member = mock(Member.class);
            Campus myCampus = mock(Campus.class);
            given(myCampus.getId()).willReturn(MY_CAMPUS_ID);
            given(member.getCampus()).willReturn(myCampus);
            given(memberRepository.findProfileById(MEMBER_ID)).willReturn(Optional.of(member));

            // when & then
            assertThatThrownBy(() ->
                    academicPlanTargetService.getDepartments(TARGET_CAMPUS_ID, MajorType.CONVERGENCE_MAJOR, MEMBER_ID))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_PARAM));
        }
    }

    @Nested
    @DisplayName("대상 전공 목록 조회")
    class GetMajors {

        @Test
        @DisplayName("DOUBLE_MAJOR 전달 시 INVALID_PARAM 예외를 던진다")
        void doubleMajor() {
            // when & then
            assertThatThrownBy(() ->
                    academicPlanTargetService.getMajors(TARGET_CAMPUS_ID, MajorType.DOUBLE_MAJOR))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_PARAM));
        }

        @Test
        @DisplayName("COMPLEX_MAJOR 전달 시 INVALID_PARAM 예외를 던진다")
        void complexMajor() {
            // when & then
            assertThatThrownBy(() ->
                    academicPlanTargetService.getMajors(TARGET_CAMPUS_ID, MajorType.COMPLEX_MAJOR))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_PARAM));
        }

        @Test
        @DisplayName("CONVERGENCE_MAJOR 요청이면 융합전공 목록을 반환한다")
        void convergenceMajor() {
            // given
            TargetMajor major1 = mock(TargetMajor.class);
            TargetMajor major2 = mock(TargetMajor.class);
            given(major1.getId()).willReturn(1L);
            given(major1.getName()).willReturn("융합전공1");
            given(major2.getId()).willReturn(2L);
            given(major2.getName()).willReturn("융합전공2");
            given(targetMajorRepository.findByCampusIdAndMajorTypeOrderByNameAsc(TARGET_CAMPUS_ID, MajorType.CONVERGENCE_MAJOR))
                    .willReturn(List.of(major1, major2));

            // when
            List<TargetMajorResponse> result =
                    academicPlanTargetService.getMajors(TARGET_CAMPUS_ID, MajorType.CONVERGENCE_MAJOR);

            // then
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("STUDENT_DESIGN 요청이면 학생설계전공 목록을 반환한다")
        void studentDesign() {
            // given
            TargetMajor major = mock(TargetMajor.class);
            given(major.getId()).willReturn(1L);
            given(major.getName()).willReturn("학생설계전공");
            given(targetMajorRepository.findByCampusIdAndMajorTypeOrderByNameAsc(TARGET_CAMPUS_ID, MajorType.STUDENT_DESIGN))
                    .willReturn(List.of(major));

            // when
            List<TargetMajorResponse> result =
                    academicPlanTargetService.getMajors(TARGET_CAMPUS_ID, MajorType.STUDENT_DESIGN);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).name()).isEqualTo("학생설계전공");
        }
    }

    @Nested
    @DisplayName("대상 허용 검증")
    class ResolveAllowedTargetName {

        private static final Long TARGET_ID = 100L;

        @Test
        @DisplayName("제한되지 않은 같은 대학 학과면 학과명을 반환한다")
        void allowedDepartment() {
            Campus myCampus = mock(Campus.class);
            given(myCampus.getId()).willReturn(MY_CAMPUS_ID);
            Member member = mock(Member.class);
            given(member.getUniversityId()).willReturn(UNIVERSITY_ID);
            given(member.getCampus()).willReturn(myCampus);
            given(member.getMemberDepartments()).willReturn(List.of());

            University university = mock(University.class);
            given(university.getId()).willReturn(UNIVERSITY_ID);
            Campus deptCampus = mock(Campus.class);
            given(deptCampus.getUniversity()).willReturn(university);
            Department department = mock(Department.class);
            given(department.getCampus()).willReturn(deptCampus);
            given(department.getName()).willReturn("경제학과");
            given(departmentRepository.findById(TARGET_ID)).willReturn(Optional.of(department));
            given(departmentRestrictionRepository.findDoubleRestrictedIdsByFromCampus(MY_CAMPUS_ID))
                    .willReturn(List.of());

            String name = academicPlanTargetService.resolveAllowedTargetName(member, MajorType.DOUBLE_MAJOR, TARGET_ID);

            assertThat(name).isEqualTo("경제학과");
        }

        @Test
        @DisplayName("다른 대학 학과면 STUDIO_TARGET_NOT_ALLOWED 예외를 던진다")
        void crossUniversityDepartment() {
            Member member = mock(Member.class);
            given(member.getUniversityId()).willReturn(UNIVERSITY_ID);

            University university = mock(University.class);
            given(university.getId()).willReturn(99L);
            Campus deptCampus = mock(Campus.class);
            given(deptCampus.getUniversity()).willReturn(university);
            Department department = mock(Department.class);
            given(department.getCampus()).willReturn(deptCampus);
            given(departmentRepository.findById(TARGET_ID)).willReturn(Optional.of(department));

            assertThatThrownBy(() ->
                    academicPlanTargetService.resolveAllowedTargetName(member, MajorType.DOUBLE_MAJOR, TARGET_ID))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.STUDIO_TARGET_NOT_ALLOWED));
        }

        @Test
        @DisplayName("제한 학과면 STUDIO_TARGET_NOT_ALLOWED 예외를 던진다")
        void restrictedDepartment() {
            Campus myCampus = mock(Campus.class);
            given(myCampus.getId()).willReturn(MY_CAMPUS_ID);
            Member member = mock(Member.class);
            given(member.getUniversityId()).willReturn(UNIVERSITY_ID);
            given(member.getCampus()).willReturn(myCampus);
            given(member.getMemberDepartments()).willReturn(List.of());

            University university = mock(University.class);
            given(university.getId()).willReturn(UNIVERSITY_ID);
            Campus deptCampus = mock(Campus.class);
            given(deptCampus.getUniversity()).willReturn(university);
            Department department = mock(Department.class);
            given(department.getCampus()).willReturn(deptCampus);
            given(departmentRepository.findById(TARGET_ID)).willReturn(Optional.of(department));
            given(departmentRestrictionRepository.findDoubleRestrictedIdsByFromCampus(MY_CAMPUS_ID))
                    .willReturn(List.of(TARGET_ID));

            assertThatThrownBy(() ->
                    academicPlanTargetService.resolveAllowedTargetName(member, MajorType.DOUBLE_MAJOR, TARGET_ID))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.STUDIO_TARGET_NOT_ALLOWED));
        }

        @Test
        @DisplayName("존재하지 않는 학과면 STUDIO_TARGET_NOT_FOUND 예외를 던진다")
        void departmentNotFound() {
            Member member = mock(Member.class);
            given(departmentRepository.findById(TARGET_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() ->
                    academicPlanTargetService.resolveAllowedTargetName(member, MajorType.DOUBLE_MAJOR, TARGET_ID))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.STUDIO_TARGET_NOT_FOUND));
        }

        @Test
        @DisplayName("타입이 맞는 같은 대학 전공이면 전공명을 반환한다")
        void allowedMajor() {
            Member member = mock(Member.class);
            given(member.getUniversityId()).willReturn(UNIVERSITY_ID);

            University university = mock(University.class);
            given(university.getId()).willReturn(UNIVERSITY_ID);
            Campus majorCampus = mock(Campus.class);
            given(majorCampus.getUniversity()).willReturn(university);
            TargetMajor major = mock(TargetMajor.class);
            given(major.getMajorType()).willReturn(MajorType.CONVERGENCE_MAJOR);
            given(major.getCampus()).willReturn(majorCampus);
            given(major.getName()).willReturn("AI융합전공");
            given(targetMajorRepository.findById(TARGET_ID)).willReturn(Optional.of(major));

            String name = academicPlanTargetService.resolveAllowedTargetName(member, MajorType.CONVERGENCE_MAJOR, TARGET_ID);

            assertThat(name).isEqualTo("AI융합전공");
        }

        @Test
        @DisplayName("요청 타입과 실제 전공 타입이 다르면 STUDIO_TARGET_NOT_ALLOWED 예외를 던진다")
        void wrongTypeMajor() {
            Member member = mock(Member.class);
            TargetMajor major = mock(TargetMajor.class);
            given(major.getMajorType()).willReturn(MajorType.STUDENT_DESIGN);
            given(targetMajorRepository.findById(TARGET_ID)).willReturn(Optional.of(major));

            assertThatThrownBy(() ->
                    academicPlanTargetService.resolveAllowedTargetName(member, MajorType.CONVERGENCE_MAJOR, TARGET_ID))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.STUDIO_TARGET_NOT_ALLOWED));
        }
    }
}
