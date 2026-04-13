package com.campusnavi.backend.global.security.jwt;

import com.campusnavi.backend.global.exception.ErrorCode;
import com.campusnavi.backend.global.exception.JwtAuthenticationException;
import com.campusnavi.backend.global.security.AuthMember;
import com.campusnavi.backend.global.security.CustomAuthenticationEntryPoint;
import com.campusnavi.backend.global.security.jwt.dto.AccessTokenPayload;
import com.campusnavi.backend.infra.redis.RedisService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private CustomAuthenticationEntryPoint entryPoint;

    @Mock
    private RedisService redisService;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtProvider, entryPoint, redisService);
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Authorization 헤더가 없으면 인증 정보 없이 다음 필터로 넘어간다")
    void noAuthorizationHeader_skipsAuthentication() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        filter.doFilter(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verify(jwtProvider, never()).parseAndValidateAccessToken(any());
    }

    @Test
    @DisplayName("Bearer 접두사가 없는 Authorization 헤더면 인증 정보 없이 다음 필터로 넘어간다")
    void noBearerPrefix_skipsAuthentication() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "NotBearer");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        filter.doFilter(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verify(jwtProvider, never()).parseAndValidateAccessToken(any());
    }

    @Test
    @DisplayName("유효한 토큰이면 SecurityContext에 인증 정보가 설정되고 다음 필터로 넘어간다")
    void validToken_setsAuthentication() throws Exception {
        // given
        String token = "valid.jwt.token";
        Long memberId = 1L;
        String role = "USER";
        AccessTokenPayload payload = new AccessTokenPayload(memberId, role, "random.jti",100L);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtProvider.parseAndValidateAccessToken(token)).thenReturn(payload);

        // when
        filter.doFilter(request, response, filterChain);

        // then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isInstanceOf(AuthMember.class);

        AuthMember authMember = (AuthMember) authentication.getPrincipal();
        assertThat(authMember.memberId()).isEqualTo(memberId);
        assertThat(authMember.role()).isEqualTo(role);
        assertThat(authentication.getAuthorities())
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER"));

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("유효하지 않은 토큰이면 SecurityContext를 초기화하고 EntryPoint를 호출한다")
    void invalidToken_clearsContextAndCallsEntryPoint() throws Exception {
        // given
        String token = "invalid.jwt.token";
        JwtAuthenticationException exception = new JwtAuthenticationException(ErrorCode.INVALID_TOKEN);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtProvider.parseAndValidateAccessToken(token)).thenThrow(exception);

        // when
        filter.doFilter(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(entryPoint).commence(request, response, exception);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("만료된 토큰이면 SecurityContext를 초기화하고 EntryPoint를 호출한다")
    void expiredToken_clearsContextAndCallsEntryPoint() throws Exception {
        // given
        String token = "expired.jwt.token";
        JwtAuthenticationException exception = new JwtAuthenticationException(ErrorCode.EXPIRED_TOKEN);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtProvider.parseAndValidateAccessToken(token)).thenThrow(exception);

        // when
        filter.doFilter(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(entryPoint).commence(request, response, exception);
        verify(filterChain, never()).doFilter(any(), any());
    }
}
