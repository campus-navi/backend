package com.campusnavi.backend.auth.service;

import com.campusnavi.backend.global.exception.BusinessException;
import com.campusnavi.backend.global.exception.ErrorCode;
import com.campusnavi.backend.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private AuthService authService;

    private static final String USERNAME = "testuser";
    private static final String NICKNAME = "testnick";

    @Test
    @DisplayName("사용 가능한 username이면 예외가 발생하지 않는다")
    void checkDuplicateUsername_notDuplicated() {
        // given
        given(memberRepository.existsByUsername(USERNAME)).willReturn(false);

        // when & then
        assertThatCode(() -> authService.checkDuplicateUsername(USERNAME))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("이미 존재하는 username이면 DUPLICATE_USERNAME 예외가 발생한다")
    void checkDuplicateUsername_duplicated() {
        // given
        given(memberRepository.existsByUsername(USERNAME)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> authService.checkDuplicateUsername(USERNAME))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_USERNAME));
    }

    @Test
    @DisplayName("사용 가능한 nickname이면 예외가 발생하지 않는다")
    void checkDuplicateNickname_notDuplicated() {
        // given
        given(memberRepository.existsByNickname(NICKNAME)).willReturn(false);

        // when & then
        assertThatCode(() -> authService.checkDuplicateNickname(NICKNAME))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("이미 존재하는 nickname이면 DUPLICATE_NICKNAME 예외가 발생한다")
    void checkDuplicateNickname_duplicated() {
        // given
        given(memberRepository.existsByNickname(NICKNAME)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> authService.checkDuplicateNickname(NICKNAME))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_NICKNAME));
    }
}
