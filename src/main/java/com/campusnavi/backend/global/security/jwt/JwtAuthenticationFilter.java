package com.campusnavi.backend.global.security.jwt;

import com.campusnavi.backend.global.exception.ErrorCode;
import com.campusnavi.backend.global.exception.JwtAuthenticationException;
import com.campusnavi.backend.global.security.AuthMember;
import com.campusnavi.backend.global.security.CustomAuthenticationEntryPoint;
import com.campusnavi.backend.global.security.jwt.dto.AccessTokenPayload;
import com.campusnavi.backend.infra.redis.RedisKeys;
import com.campusnavi.backend.infra.redis.RedisService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER = "Bearer ";
    private static final String MDC_MEMBER_ID = "memberId";

    private final JwtProvider jwtProvider;
    private final CustomAuthenticationEntryPoint entryPoint;
    private final RedisService redisService;


    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        try{
            String token = extractToken(request);

            if (token != null){

                AccessTokenPayload payload = jwtProvider.parseAndValidateAccessToken(token);

                if (redisService.hasKey(RedisKeys.blacklist(payload.jti()))){
                    throw new JwtAuthenticationException(ErrorCode.BLACKLISTED_TOKEN);
                }

                Long memberId = payload.memberId();
                String role = payload.role();
                MDC.put(MDC_MEMBER_ID, memberId.toString());

                AuthMember authMember = new AuthMember(memberId, role, payload.universityId());

                Authentication authentication =
                        new UsernamePasswordAuthenticationToken(
                                authMember,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
            filterChain.doFilter(request, response);
        } catch (JwtAuthenticationException e){
            SecurityContextHolder.clearContext();
            MDC.remove(MDC_MEMBER_ID);
            entryPoint.commence(request, response, e);
        }
    }

    private String extractToken(HttpServletRequest request) {
        String token = request.getHeader(AUTHORIZATION_HEADER);
        if (token != null && token.startsWith(BEARER)) {
            return token.substring(7);
        }
        return null;
    }
}
