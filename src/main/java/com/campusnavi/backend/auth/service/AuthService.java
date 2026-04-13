package com.campusnavi.backend.auth.service;

import com.campusnavi.backend.global.exception.BusinessException;
import com.campusnavi.backend.global.exception.ErrorCode;
import com.campusnavi.backend.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;

    public void checkDuplicateUsername(String username){
        if (memberRepository.existsByUsername(username)){
            throw new BusinessException(ErrorCode.DUPLICATE_USERNAME);
        }
    }

    public void checkDuplicateNickname(String nickname){
        if (memberRepository.existsByNickname(nickname)){
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }
    }
}
