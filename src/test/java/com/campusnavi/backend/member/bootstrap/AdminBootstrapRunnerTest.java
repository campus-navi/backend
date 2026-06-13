package com.campusnavi.backend.member.bootstrap;

import com.campusnavi.backend.member.entity.Member;
import com.campusnavi.backend.member.entity.MemberRole;
import com.campusnavi.backend.member.repository.MemberRepository;
import com.campusnavi.backend.university.repository.UniversityRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapRunnerTest {

    @Mock
    private AdminBootstrapProperties properties;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private UniversityRepository universityRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminBootstrapRunner runner;

    @Test
    @DisplayName("관리자 부트스트랩이 비활성화되어 있으면 아무 작업도 하지 않는다")
    void disabled() {
        // given
        given(properties.enabled()).willReturn(false);

        // when & then
        assertThatCode(() -> runner.run(mock(ApplicationArguments.class)))
                .doesNotThrowAnyException();
        then(memberRepository).should(never()).existsByRoleAndUniversityId(any(), any());
        then(universityRepository).should(never()).existsById(any());
        then(memberRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("같은 대학교의 관리자가 이미 있으면 새로 저장하지 않는다")
    void existingAdmin() {
        // given
        given(properties.enabled()).willReturn(true);
        given(properties.universityId()).willReturn(1L);
        given(memberRepository.existsByRoleAndUniversityId(MemberRole.ADMIN, 1L)).willReturn(true);

        // when & then
        assertThatCode(() -> runner.run(mock(ApplicationArguments.class)))
                .doesNotThrowAnyException();
        then(universityRepository).should(never()).existsById(any());
        then(memberRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("설정된 대학교가 없으면 예외가 발생한다")
    void universityNotFound() {
        // given
        given(properties.enabled()).willReturn(true);
        given(properties.universityId()).willReturn(99L);
        given(memberRepository.existsByRoleAndUniversityId(MemberRole.ADMIN, 99L)).willReturn(false);
        given(universityRepository.existsById(99L)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> runner.run(mock(ApplicationArguments.class)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("universityId=99");
        then(memberRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("정상 조건이면 비밀번호를 인코딩해 관리자 계정을 저장한다")
    void createAdmin() throws Exception {
        // given
        given(properties.enabled()).willReturn(true);
        given(properties.universityId()).willReturn(1L);
        given(properties.email()).willReturn("admin@example.com");
        given(properties.username()).willReturn("admin");
        given(properties.password()).willReturn("password123");
        given(properties.nickname()).willReturn("관리자");
        given(memberRepository.existsByRoleAndUniversityId(MemberRole.ADMIN, 1L)).willReturn(false);
        given(universityRepository.existsById(1L)).willReturn(true);
        given(passwordEncoder.encode("password123")).willReturn("encoded-password");

        // when
        runner.run(mock(ApplicationArguments.class));

        // then
        ArgumentCaptor<Member> captor = ArgumentCaptor.forClass(Member.class);
        then(memberRepository).should().save(captor.capture());
        Member saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("admin@example.com");
        assertThat(saved.getUsername()).isEqualTo("admin");
        assertThat(saved.getPassword()).isEqualTo("encoded-password");
        assertThat(saved.getNickname()).isEqualTo("관리자");
        assertThat(saved.getUniversityId()).isEqualTo(1L);
        assertThat(saved.getRole()).isEqualTo(MemberRole.ADMIN);
    }
}
