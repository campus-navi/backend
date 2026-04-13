package com.campusnavi.backend.auth.service;

import com.campusnavi.backend.auth.dto.EmailSendRequest;
import com.campusnavi.backend.auth.dto.EmailVerifyRequest;
import com.campusnavi.backend.auth.dto.VerifiedTokenResponse;
import com.campusnavi.backend.global.exception.BusinessException;
import com.campusnavi.backend.global.exception.ErrorCode;
import com.campusnavi.backend.infra.email.EmailSender;
import com.campusnavi.backend.infra.redis.RedisKeys;
import com.campusnavi.backend.infra.redis.RedisService;
import com.campusnavi.backend.member.repository.MemberRepository;
import com.campusnavi.backend.university.entity.Campus;
import com.campusnavi.backend.university.repository.CampusRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock
    private EmailSender emailSender;

    @Mock
    private RedisService redisService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private CampusRepository campusRepository;

    @InjectMocks
    private EmailVerificationService emailVerificationService;

    private static final String IP = "127.0.0.1";
    private static final String EMAIL = "example@test.ac.kr";
    private static final String CODE = "123456";
    private static final EmailSendRequest REQUEST = new EmailSendRequest(1L, EMAIL);
    private static final EmailVerifyRequest VERIFY_REQUEST = new EmailVerifyRequest(EMAIL, CODE);

    @Test
    @DisplayName("정상요청이면 인증코드와 쿨다운을 저장하고 이메일을 발송한다")
    void sendEmailVerification_success(){
        // given
        Campus campus = mock(Campus.class);


        given(redisService.hasKey(RedisKeys.emailBlockIp(IP))).willReturn(false);
        given(redisService.increment(RedisKeys.emailRequestIp(IP))).willReturn(1L);
        given(redisService.hasKey(RedisKeys.emailCooldown(EMAIL))).willReturn(false);
        given(memberRepository.existsByEmail(EMAIL)).willReturn(false);
        given(campusRepository.findById(1L)).willReturn(Optional.of(campus));
        given(campus.getDomain()).willReturn("test.ac.kr");

        // when
        emailVerificationService.sendEmailVerification(REQUEST, IP);

        // then
        then(redisService).should().expire(eq(RedisKeys.emailRequestIp(IP)), any());
        then(redisService).should().set(eq(RedisKeys.emailCode(EMAIL)), anyString(), any());
        then(redisService).should().set(eq(RedisKeys.emailCooldown(EMAIL)), eq("cooldown"), any());
        then(emailSender).should().send(eq(EMAIL), anyString(), anyString());
    }

    @Test
    @DisplayName("이미 차단된 IP면 IP_BLOCKED 예외가 발생한다")
    void sendEmailVerification_ipAlreadyBlocked() {
        // given


        given(redisService.hasKey(RedisKeys.emailBlockIp(IP))).willReturn(true);

        // when & then
        assertThatThrownBy(() -> emailVerificationService.sendEmailVerification(REQUEST, IP))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.IP_BLOCKED));
    }

    @Test
    @DisplayName("IP 요청횟수가 초과되면 block key에 저장하고 IP_BLOCKED 예외가 발생한다")
    void sendEmailVerification_ipRequestExceeded() {
        // given


        given(redisService.hasKey(RedisKeys.emailBlockIp(IP))).willReturn(false);
        given(redisService.increment(RedisKeys.emailRequestIp(IP))).willReturn(6L);

        // when & then
        assertThatThrownBy(() -> emailVerificationService.sendEmailVerification(REQUEST, IP))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.IP_BLOCKED));

        then(redisService).should().set(eq(RedisKeys.emailBlockIp(IP)), eq("blocked"), any());
    }

    @Test
    @DisplayName("이메일 쿨다운 중 요청이 오면 EMAIL_SEND_COOLDOWN 예외가 발생한다")
    void sendEmailVerification_emailCooldown() {
        // given


        given(redisService.hasKey(RedisKeys.emailBlockIp(IP))).willReturn(false);
        given(redisService.increment(RedisKeys.emailRequestIp(IP))).willReturn(1L);
        given(redisService.hasKey(RedisKeys.emailCooldown(EMAIL))).willReturn(true);

        // when & then
        assertThatThrownBy(() -> emailVerificationService.sendEmailVerification(REQUEST, IP))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.EMAIL_SEND_COOLDOWN));
    }

    @Test
    @DisplayName("중복된 이메일에 요청이 오면 DUPLICATE_EMAIL 예외가 발생한다")
    void sendEmailVerification_duplicateEmail() {
        // given


        given(redisService.hasKey(RedisKeys.emailBlockIp(IP))).willReturn(false);
        given(redisService.increment(RedisKeys.emailRequestIp(IP))).willReturn(1L);
        given(redisService.hasKey(RedisKeys.emailCooldown(EMAIL))).willReturn(false);
        given(memberRepository.existsByEmail(EMAIL)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> emailVerificationService.sendEmailVerification(REQUEST, IP))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_EMAIL));
    }

    @Test
    @DisplayName("campusId로 캠퍼스가 조회되지 않으면 CAMPUS_NOT_FOUND 예외가 발생한다")
    void sendEmailVerification_campusNotFound() {
        // given


        given(redisService.hasKey(RedisKeys.emailBlockIp(IP))).willReturn(false);
        given(redisService.increment(RedisKeys.emailRequestIp(IP))).willReturn(1L);
        given(redisService.hasKey(RedisKeys.emailCooldown(EMAIL))).willReturn(false);
        given(memberRepository.existsByEmail(EMAIL)).willReturn(false);
        given(campusRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> emailVerificationService.sendEmailVerification(REQUEST, IP))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.CAMPUS_NOT_FOUND));
    }

    @Test
    @DisplayName("campusId로 조회한 캠퍼스의 도메인과 요청 이메일의 도메인이 불일치하면 DOMAIN_MISMATCH 예외가 발생한다")
    void sendEmailVerification_domainMismatch() {
        // given
        Campus campus = mock(Campus.class);


        given(redisService.hasKey(RedisKeys.emailBlockIp(IP))).willReturn(false);
        given(redisService.increment(RedisKeys.emailRequestIp(IP))).willReturn(1L);
        given(redisService.hasKey(RedisKeys.emailCooldown(EMAIL))).willReturn(false);
        given(memberRepository.existsByEmail(EMAIL)).willReturn(false);
        given(campusRepository.findById(1L)).willReturn(Optional.of(campus));
        given(campus.getDomain()).willReturn("other.ac.kr");

        // when & then
        assertThatThrownBy(() -> emailVerificationService.sendEmailVerification(REQUEST, IP))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.DOMAIN_MISMATCH));
    }

    @Test
    @DisplayName("코드가 일치하면 기존 코드를 삭제하고 verifiedToken을 저장한 뒤 반환한다")
    void verifyEmailCode_success() {
        // given
        given(redisService.get(RedisKeys.emailCode(EMAIL))).willReturn(CODE);

        // when
        VerifiedTokenResponse response = emailVerificationService.verifyEmailCode(VERIFY_REQUEST);

        // then
        then(redisService).should().delete(RedisKeys.emailCode(EMAIL));
        then(redisService).should().set(eq(RedisKeys.emailVerified(response.verifiedToken())), eq(EMAIL), any());
        assertThat(response.verifiedToken()).isNotBlank();
    }

    @Test
    @DisplayName("Redis에 코드가 없으면 EMAIL_CODE_NOT_FOUND 예외가 발생한다")
    void verifyEmailCode_codeNotFound() {
        // given
        given(redisService.get(RedisKeys.emailCode(EMAIL))).willReturn(null);

        // when & then
        assertThatThrownBy(() -> emailVerificationService.verifyEmailCode(VERIFY_REQUEST))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.EMAIL_CODE_NOT_FOUND));
    }

    @Test
    @DisplayName("코드가 불일치하면 EMAIL_CODE_INVALID 예외가 발생한다")
    void verifyEmailCode_codeInvalid() {
        // given
        given(redisService.get(RedisKeys.emailCode(EMAIL))).willReturn("999999");

        // when & then
        assertThatThrownBy(() -> emailVerificationService.verifyEmailCode(VERIFY_REQUEST))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.EMAIL_CODE_INVALID));
    }
}