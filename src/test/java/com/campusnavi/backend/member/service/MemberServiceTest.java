package com.campusnavi.backend.member.service;

import com.campusnavi.backend.global.exception.BusinessException;
import com.campusnavi.backend.global.exception.ErrorCode;
import com.campusnavi.backend.global.security.AuthMember;
import com.campusnavi.backend.tag.entity.Tag;
import com.campusnavi.backend.tag.repository.TagRepository;
import com.campusnavi.backend.member.dto.StudentNumberUpdateRequest;
import com.campusnavi.backend.member.dto.GradeUpdateRequest;
import com.campusnavi.backend.member.dto.MemberInterestUpdateRequest;
import com.campusnavi.backend.member.dto.PasswordUpdateRequest;
import com.campusnavi.backend.member.dto.UsernameUpdateRequest;
import com.campusnavi.backend.member.entity.Member;
import com.campusnavi.backend.member.entity.MemberStatus;
import com.campusnavi.backend.member.repository.MemberInterestRepository;
import com.campusnavi.backend.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MemberInterestRepository memberInterestRepository;

    @Mock
    private TagRepository tagRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private MemberService memberService;

    private static final Long MEMBER_ID = 1L;
    private static final AuthMember AUTH_MEMBER = new AuthMember(MEMBER_ID, "USER", 10L);

    @Nested
    @DisplayName("관심사 전체 교체")
    class UpdateMemberInterests {

        @Test
        @DisplayName("정상 요청이면 기존 관심사를 삭제하고 새로 저장한다")
        void success() {
            // given
            List<Long> interestIds = List.of(1L, 2L);
            MemberInterestUpdateRequest request = new MemberInterestUpdateRequest(interestIds);
            Tag tag1 = mock(Tag.class);
            Tag tag2 = mock(Tag.class);
            given(tagRepository.findAllById(interestIds)).willReturn(List.of(tag1, tag2));

            // when & then
            assertThatCode(() -> memberService.updateMemberInterests(AUTH_MEMBER, request))
                    .doesNotThrowAnyException();
            then(memberInterestRepository).should().deleteAllByMemberId(MEMBER_ID);
            then(memberInterestRepository).should().saveAll(any());
        }

        @Test
        @DisplayName("빈 배열을 전달하면 기존 관심사를 전부 해제하고 저장은 하지 않는다")
        void emptyList() {
            // given
            MemberInterestUpdateRequest request = new MemberInterestUpdateRequest(List.of());

            // when & then
            assertThatCode(() -> memberService.updateMemberInterests(AUTH_MEMBER, request))
                    .doesNotThrowAnyException();
            then(memberInterestRepository).should().deleteAllByMemberId(MEMBER_ID);
            then(memberInterestRepository).should(never()).saveAll(any());
        }

        @Test
        @DisplayName("존재하지 않는 tagId가 포함되면 TAG_NOT_FOUND 예외가 발생한다")
        void tagNotFound() {
            // given
            List<Long> interestIds = List.of(1L, 999L);
            MemberInterestUpdateRequest request = new MemberInterestUpdateRequest(interestIds);
            given(tagRepository.findAllById(interestIds)).willReturn(List.of(mock(Tag.class)));

            // when & then
            assertThatThrownBy(() -> memberService.updateMemberInterests(AUTH_MEMBER, request))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.TAG_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("아이디 변경")
    class ChangeUsername {

        @Test
        @DisplayName("중복되지 않으면 아이디를 변경한다")
        void success() {
            // given
            Member member = mock(Member.class);
            UsernameUpdateRequest request = new UsernameUpdateRequest("newname1");
            given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
            given(memberRepository.existsByUsername("newname1")).willReturn(false);

            // when
            memberService.changeUsername(MEMBER_ID, request);

            // then
            then(member).should().changeUsername("newname1");
        }

        @Test
        @DisplayName("존재하지 않는 회원이면 MEMBER_NOT_FOUND 예외가 발생한다")
        void memberNotFound() {
            // given
            UsernameUpdateRequest request = new UsernameUpdateRequest("newname1");
            given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> memberService.changeUsername(MEMBER_ID, request))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.MEMBER_NOT_FOUND));
        }

        @Test
        @DisplayName("이미 사용 중인 아이디면 DUPLICATE_USERNAME 예외가 발생한다")
        void duplicateUsername() {
            // given
            Member member = mock(Member.class);
            UsernameUpdateRequest request = new UsernameUpdateRequest("dupname1");
            given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
            given(memberRepository.existsByUsername("dupname1")).willReturn(true);

            // when & then
            assertThatThrownBy(() -> memberService.changeUsername(MEMBER_ID, request))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_USERNAME));
            then(member).should(never()).changeUsername(any());
        }
    }

    @Nested
    @DisplayName("비밀번호 변경")
    class ChangePassword {

        @Test
        @DisplayName("새 비밀번호를 인코딩하여 저장한다")
        void success() {
            // given
            Member member = mock(Member.class);
            PasswordUpdateRequest request = new PasswordUpdateRequest("newpass1!");
            given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
            given(passwordEncoder.encode("newpass1!")).willReturn("encoded");

            // when
            memberService.changePassword(MEMBER_ID, request);

            // then
            then(member).should().changePassword("encoded");
        }

        @Test
        @DisplayName("존재하지 않는 회원이면 MEMBER_NOT_FOUND 예외가 발생한다")
        void memberNotFound() {
            // given
            PasswordUpdateRequest request = new PasswordUpdateRequest("newpass1!");
            given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> memberService.changePassword(MEMBER_ID, request))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.MEMBER_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("입학년도 및 학번 변경")
    class ChangeStudentNumberWithAdmissionYear {

        @Test
        @DisplayName("입학년도와 학번을 함께 변경한다")
        void success() {
            // given
            Member member = mock(Member.class);
            StudentNumberUpdateRequest request = new StudentNumberUpdateRequest(2025, "20251234");
            given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));

            // when
            memberService.changeStudentNumber(MEMBER_ID, request);

            // then
            then(member).should().changeStudentNumber(2025, "20251234");
        }

        @Test
        @DisplayName("존재하지 않는 회원이면 MEMBER_NOT_FOUND 예외가 발생한다")
        void memberNotFound() {
            // given
            StudentNumberUpdateRequest request = new StudentNumberUpdateRequest(2025, "20251234");
            given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> memberService.changeStudentNumber(MEMBER_ID, request))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.MEMBER_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("학년 변경")
    class ChangeGrade {

        @Test
        @DisplayName("학년을 변경한다")
        void success() {
            // given
            Member member = mock(Member.class);
            GradeUpdateRequest request = new GradeUpdateRequest(2);
            given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));

            // when
            memberService.changeGrade(MEMBER_ID, request);

            // then
            then(member).should().changeGrade(2);
        }

        @Test
        @DisplayName("존재하지 않는 회원이면 MEMBER_NOT_FOUND 예외가 발생한다")
        void memberNotFound() {
            // given
            GradeUpdateRequest request = new GradeUpdateRequest(2);
            given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> memberService.changeGrade(MEMBER_ID, request))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.MEMBER_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("회원탈퇴")
    class Withdraw {

        @Test
        @DisplayName("회원을 익명화하고 WITHDRAWN 상태로 변경한다")
        void success() {
            // given
            Member member = Member.join("user@test.ac.kr", "username1", "encoded",
                    "닉네임", "홍길동", "20260001", 10L, null, 2025, 1);
            given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));

            // when
            memberService.withdraw(MEMBER_ID);

            // then
            assertThat(member.getStatus()).isEqualTo(MemberStatus.WITHDRAWN);
            assertThat(member.getDeletedAt()).isNotNull();
            assertThat(member.getUsername()).startsWith("[탈퇴된 회원");
            assertThat(member.getEmail()).endsWith("@withdrawn");
            assertThat(member.getNickname()).startsWith("[탈퇴된 회원");
            assertThat(member.getName()).startsWith("[탈퇴된 회원");
            assertThat(member.getStudentNumber()).isNull();
            assertThat(member.getPassword()).isNotEqualTo("encoded");
        }

        @Test
        @DisplayName("존재하지 않는 회원이면 MEMBER_NOT_FOUND 예외가 발생한다")
        void memberNotFound() {
            // given
            given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> memberService.withdraw(MEMBER_ID))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.MEMBER_NOT_FOUND));
        }
    }
}
