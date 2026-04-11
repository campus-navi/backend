package com.campusnavi.backend.auth.service;

import com.campusnavi.backend.auth.dto.EmailSendRequest;
import com.campusnavi.backend.global.exception.BusinessException;
import com.campusnavi.backend.global.exception.ErrorCode;
import com.campusnavi.backend.infra.email.EmailSender;
import com.campusnavi.backend.infra.email.EmailTemplate;
import com.campusnavi.backend.infra.redis.RedisKeys;
import com.campusnavi.backend.infra.redis.RedisService;
import com.campusnavi.backend.member.repository.MemberRepository;
import com.campusnavi.backend.university.entity.Campus;
import com.campusnavi.backend.university.repository.CampusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final EmailSender emailSender;
    private final CampusRepository campusRepository;
    private final RedisService redisService;
    private final MemberRepository memberRepository;

    private static final Duration EMAIL_CODE_EXPIRATION = Duration.ofMinutes(5);
    private static final Duration EMAIL_COOLDOWN_EXPIRATION = Duration.ofMinutes(1);
    private static final int MAX_REQUESTS_PER_IP = 5;
    private static final Duration IP_COUNT_TTL = Duration.ofHours(1);
    private static final Duration IP_BLOCK_DURATION = Duration.ofHours(24);

    public void sendEmailVerification(EmailSendRequest request, String ip) {
        if (redisService.hasKey(RedisKeys.emailBlockIp(ip))) {
            throw new BusinessException(ErrorCode.IP_BLOCKED);
        }

        // IP 요청 횟수 증가 (유효성 검사 전 모든 시도 카운팅)
        Long requestCount = redisService.increment(RedisKeys.emailRequestIp(ip));
        if (requestCount == 1) {
            redisService.expire(RedisKeys.emailRequestIp(ip), IP_COUNT_TTL);
        }
        if (requestCount > MAX_REQUESTS_PER_IP) {
            redisService.set(RedisKeys.emailBlockIp(ip), "blocked", IP_BLOCK_DURATION);
            throw new BusinessException(ErrorCode.IP_BLOCKED);
        }

        String email = request.email();

        if (redisService.hasKey(RedisKeys.emailCooldown(email))) {
            throw new BusinessException(ErrorCode.EMAIL_SEND_COOLDOWN);
        }

        if (memberRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        Campus campus = campusRepository.findById(request.campusId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CAMPUS_NOT_FOUND));

        String requestDomain = email.split("@")[1];

        if (!campus.getDomain().equals(requestDomain)) {
            throw new BusinessException(ErrorCode.DOMAIN_MISMATCH);
        }

        String code = generateCode().toString();
        EmailTemplate template = EmailTemplate.sendCode(code);

        redisService.set(RedisKeys.emailCode(email), code, EMAIL_CODE_EXPIRATION);
        redisService.set(RedisKeys.emailCooldown(email), "cooldown", EMAIL_COOLDOWN_EXPIRATION);

        emailSender.send(email, template.subject(), template.content());
    }

    private Integer generateCode() {
        SecureRandom random = new SecureRandom();
        return random.nextInt(900000) + 100000;
    }
}
