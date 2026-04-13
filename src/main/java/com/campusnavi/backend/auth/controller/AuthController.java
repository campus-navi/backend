package com.campusnavi.backend.auth.controller;

import com.campusnavi.backend.auth.dto.*;
import com.campusnavi.backend.auth.service.AuthService;
import com.campusnavi.backend.auth.service.EmailVerificationService;
import com.campusnavi.backend.global.response.ApiResponse;
import com.campusnavi.backend.global.util.cookie.RefreshTokenCookieProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final EmailVerificationService emailVerificationService;
    private final AuthService authService;
    private final RefreshTokenCookieProvider cookieProvider;

    @PostMapping("/email/send")
    public ResponseEntity<ApiResponse<Void>> sendEmailVerificationCode(@RequestBody @Valid EmailSendRequest sendRequest, HttpServletRequest request) {
        // TODO: 배포 환경 확정 후 X-Forwarded-For, X-Real-IP 헤더 처리 추가
        String ip = request.getRemoteAddr();
        emailVerificationService.sendEmailVerification(sendRequest, ip);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @PostMapping("/email/verify")
    public ResponseEntity<ApiResponse<VerifiedTokenResponse>> verifyEmailCode(@RequestBody @Valid EmailVerifyRequest verifyRequest) {
        VerifiedTokenResponse response = emailVerificationService.verifyEmailCode(verifyRequest);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/check-username")
    public ResponseEntity<ApiResponse<Void>> checkUsername(@RequestParam String username) {
        authService.checkDuplicateUsername(username);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @GetMapping("/check-nickname")
    public ResponseEntity<ApiResponse<Void>> checkNickname(@RequestParam String nickname) {
        authService.checkDuplicateNickname(nickname);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> signUp(@RequestBody @Valid SignUpRequest request) {
        TokenResponse token = authService.signUp(request);

        ResponseCookie responseCookie = cookieProvider.setRefreshTokenCookie(token.refreshToken());

        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, responseCookie.toString())
                .header(HttpHeaders.AUTHORIZATION,token.accessToken())
                .body(ApiResponse.ok());
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Void>> login(@RequestBody @Valid LoginRequest request) {
        TokenResponse token = authService.login(request);

        ResponseCookie responseCookie = cookieProvider.setRefreshTokenCookie(token.refreshToken());

        return ResponseEntity.status(HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE, responseCookie.toString())
                .header(HttpHeaders.AUTHORIZATION,token.accessToken())
                .body(ApiResponse.ok());
    }

    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<Void>> reissue(@CookieValue(value = "refreshToken", required = false) String refreshToken) {
        TokenResponse token = authService.reissue(refreshToken);

        ResponseCookie responseCookie = cookieProvider.setRefreshTokenCookie(token.refreshToken());

        return ResponseEntity.status(HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE, responseCookie.toString())
                .header(HttpHeaders.AUTHORIZATION,token.accessToken())
                .body(ApiResponse.ok());
    }
}
