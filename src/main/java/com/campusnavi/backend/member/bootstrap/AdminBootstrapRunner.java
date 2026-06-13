package com.campusnavi.backend.member.bootstrap;

import com.campusnavi.backend.member.entity.Member;
import com.campusnavi.backend.member.entity.MemberRole;
import com.campusnavi.backend.member.repository.MemberRepository;
import com.campusnavi.backend.university.repository.UniversityRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class AdminBootstrapRunner implements ApplicationRunner {

    private final AdminBootstrapProperties properties;
    private final MemberRepository memberRepository;
    private final UniversityRepository universityRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(@NonNull ApplicationArguments args) {
        if (!properties.enabled()) return;
        if (memberRepository.existsByRoleAndUniversityId(MemberRole.ADMIN, properties.universityId())) return;

        if (!universityRepository.existsById(properties.universityId())) {
            throw new IllegalStateException("Admin 생성 실패: universityId=" + properties.universityId() + " 를 찾을 수 없음");
        }

        memberRepository.save(Member.createAdmin(
                properties.email(),
                properties.username(),
                passwordEncoder.encode(properties.password()),
                properties.nickname(),
                properties.universityId()
        ));
    }
}
