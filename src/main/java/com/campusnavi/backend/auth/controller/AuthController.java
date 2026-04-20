package com.campusnavi.backend.auth.controller;

import com.campusnavi.backend.auth.dto.*;
import com.campusnavi.backend.auth.service.AuthService;
import com.campusnavi.backend.auth.service.EmailVerificationService;
import com.campusnavi.backend.global.response.ApiResponse;
import com.campusnavi.backend.global.response.ErrorResponse;
import com.campusnavi.backend.global.util.cookie.RefreshTokenCookieProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "2. 인증", description = "회원가입, 로그인, 토큰 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final EmailVerificationService emailVerificationService;
    private final AuthService authService;
    private final RefreshTokenCookieProvider cookieProvider;

    @Operation(summary = "이메일 인증코드 발송", description = "학교 이메일로 6자리 인증코드를 발송합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "발송 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효하지 않은 이메일 형식",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 가입된 이메일",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "쿨다운 또는 IP 차단",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "이메일 발송 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/email/send")
    public ResponseEntity<ApiResponse<Void>> sendEmailVerificationCode(@RequestBody @Valid EmailSendRequest sendRequest, HttpServletRequest request) {
        // TODO: 배포 환경 확정 후 X-Forwarded-For, X-Real-IP 헤더 처리 추가
        String ip = request.getRemoteAddr();
        emailVerificationService.sendEmailVerification(sendRequest, ip);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(summary = "이메일 인증코드 확인", description = "인증코드 검증 후 회원가입에 사용할 verifiedToken을 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "인증 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "인증코드 불일치 또는 만료",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/email/verify")
    public ResponseEntity<ApiResponse<VerifiedTokenResponse>> verifyEmailCode(@RequestBody @Valid EmailVerifyRequest verifyRequest) {
        VerifiedTokenResponse response = emailVerificationService.verifyEmailCode(verifyRequest);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(summary = "아이디 중복 확인")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "사용 가능"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 사용 중인 아이디",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/check-username")
    public ResponseEntity<ApiResponse<Void>> checkUsername(@RequestParam String username) {
        authService.checkDuplicateUsername(username);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(summary = "닉네임 중복 확인")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "사용 가능"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 사용 중인 닉네임",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/check-nickname")
    public ResponseEntity<ApiResponse<Void>> checkNickname(@RequestParam String nickname) {
        authService.checkDuplicateNickname(nickname);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(summary = "회원가입", description = "회원가입 완료 후 자동 로그인됩니다. Access Token은 Authorization 헤더, Refresh Token은 HttpOnly 쿠키로 반환됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "회원가입 성공",
                    headers = {
                            @Header(name = "Authorization", description = "{accessToken} (Bearer prefix 없는 raw JWT, 요청 시 직접 Bearer 붙여서 사용)", schema = @Schema(type = "string")),
                            @Header(name = "Set-Cookie", description = "refreshToken={refreshToken}; HttpOnly; Path=/", schema = @Schema(type = "string"))
                    }),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 유효성 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "인증되지 않은 이메일",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 학과",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "중복된 아이디/닉네임/이메일",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> signUp(@RequestBody @Valid SignUpRequest request) {
        TokenResponse token = authService.signUp(request);

        ResponseCookie responseCookie = cookieProvider.setRefreshTokenCookie(token.refreshToken());

        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, responseCookie.toString())
                .header(HttpHeaders.AUTHORIZATION, token.accessToken())
                .body(ApiResponse.ok());
    }

    @Operation(summary = "로그인", description = "Access Token은 Authorization 헤더, Refresh Token은 HttpOnly 쿠키로 반환됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공",
                    headers = {
                            @Header(name = "Authorization", description = "{accessToken} (Bearer prefix 없는 raw JWT, 요청 시 직접 Bearer 붙여서 사용)", schema = @Schema(type = "string")),
                            @Header(name = "Set-Cookie", description = "refreshToken={refreshToken}; HttpOnly; Path=/", schema = @Schema(type = "string"))
                    }),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "아이디 또는 비밀번호 불일치",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Void>> login(@RequestBody @Valid LoginRequest request) {
        TokenResponse token = authService.login(request);

        ResponseCookie responseCookie = cookieProvider.setRefreshTokenCookie(token.refreshToken());

        return ResponseEntity.status(HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE, responseCookie.toString())
                .header(HttpHeaders.AUTHORIZATION, token.accessToken())
                .body(ApiResponse.ok());
    }

    @Operation(summary = "토큰 재발급", description = "쿠키의 Refresh Token으로 Access/Refresh Token을 재발급합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "재발급 성공",
                    headers = {
                            @Header(name = "Authorization", description = "{accessToken} (Bearer prefix 없는 raw JWT, 요청 시 직접 Bearer 붙여서 사용)", schema = @Schema(type = "string")),
                            @Header(name = "Set-Cookie", description = "refreshToken={refreshToken}; HttpOnly; Path=/", schema = @Schema(type = "string"))
                    }),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "유효하지 않거나 만료된 Refresh Token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<Void>> reissue(
            @Parameter(in = ParameterIn.COOKIE, name = "refreshToken", description = "Refresh Token", required = true)
            @CookieValue(value = "refreshToken", required = false) String refreshToken) {
        TokenResponse token = authService.reissue(refreshToken);

        ResponseCookie responseCookie = cookieProvider.setRefreshTokenCookie(token.refreshToken());

        return ResponseEntity.status(HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE, responseCookie.toString())
                .header(HttpHeaders.AUTHORIZATION, token.accessToken())
                .body(ApiResponse.ok());
    }

    @Operation(summary = "로그아웃", description = "Access Token을 블랙리스트에 등록하고 Refresh Token을 삭제합니다. 토큰이 없거나 유효하지 않아도 200을 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그아웃 성공",
                    headers = {
                            @Header(name = "Set-Cookie", description = "refreshToken=; Max-Age=0 (쿠키 만료)", schema = @Schema(type = "string"))
                    })
    })
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Parameter(in = ParameterIn.HEADER, name = "Authorization", description = "Bearer {accessToken}")
            @RequestHeader(value = "Authorization", required = false) String accessToken,
            @Parameter(in = ParameterIn.COOKIE, name = "refreshToken", description = "Refresh Token")
            @CookieValue(value = "refreshToken", required = false) String refreshToken) {
        authService.logout(accessToken, refreshToken);

        ResponseCookie responseCookie = cookieProvider.expireRefreshTokenCookie();
        return ResponseEntity.status(HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE, responseCookie.toString())
                .body(ApiResponse.ok());
    }
}
