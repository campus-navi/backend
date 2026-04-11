package com.campusnavi.backend.auth.controller;

import com.campusnavi.backend.auth.dto.EmailSendRequest;
import com.campusnavi.backend.auth.service.EmailVerificationService;
import com.campusnavi.backend.global.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final EmailVerificationService emailVerificationService;

    @PostMapping("/email/send")
    public ResponseEntity<ApiResponse<Void>> sendEmailVerificationCode(@RequestBody EmailSendRequest sendRequest, HttpServletRequest request) {
        // TODO: 배포 환경 확정 후 X-Forwarded-For, X-Real-IP 헤더 처리 추가
        String ip = request.getRemoteAddr();
        emailVerificationService.sendEmailVerification(sendRequest, ip);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
