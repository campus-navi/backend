package com.campusnavi.backend.member.service;

import com.campusnavi.backend.global.exception.BusinessException;
import com.campusnavi.backend.global.exception.ErrorCode;
import com.campusnavi.backend.global.security.AuthMember;
import com.campusnavi.backend.tag.entity.Tag;
import com.campusnavi.backend.tag.repository.TagRepository;
import com.campusnavi.backend.member.dto.MemberInterestUpdateRequest;
import com.campusnavi.backend.member.repository.MemberInterestRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberInterestRepository memberInterestRepository;

    @Mock
    private TagRepository tagRepository;

    @InjectMocks
    private MemberService memberService;

    private static final Long MEMBER_ID = 1L;
    private static final AuthMember AUTH_MEMBER = new AuthMember(MEMBER_ID, "USER", 10L);

    @Nested
    @DisplayName("관심사 전체 교체")
    class UpdateMemberInterests {

        @Test
        @DisplayName("정상 요청이면 기존 관심사를 삭제하고 새로 저장한다")
        void success() {
            // given
            List<Long> interestIds = List.of(1L, 2L);
            MemberInterestUpdateRequest request = new MemberInterestUpdateRequest(interestIds);
            Tag tag1 = mock(Tag.class);
            Tag tag2 = mock(Tag.class);
            given(tagRepository.findAllById(interestIds)).willReturn(List.of(tag1, tag2));

            // when & then
            assertThatCode(() -> memberService.updateMemberInterests(AUTH_MEMBER, request))
                    .doesNotThrowAnyException();
            then(memberInterestRepository).should().deleteAllByMemberId(MEMBER_ID);
            then(memberInterestRepository).should().saveAll(any());
        }

        @Test
        @DisplayName("빈 배열을 전달하면 기존 관심사를 전부 해제하고 저장은 하지 않는다")
        void emptyList() {
            // given
            MemberInterestUpdateRequest request = new MemberInterestUpdateRequest(List.of());

            // when & then
            assertThatCode(() -> memberService.updateMemberInterests(AUTH_MEMBER, request))
                    .doesNotThrowAnyException();
            then(memberInterestRepository).should().deleteAllByMemberId(MEMBER_ID);
            then(memberInterestRepository).should(never()).saveAll(any());
        }

        @Test
        @DisplayName("존재하지 않는 tagId가 포함되면 TAG_NOT_FOUND 예외가 발생한다")
        void tagNotFound() {
            // given
            List<Long> interestIds = List.of(1L, 999L);
            MemberInterestUpdateRequest request = new MemberInterestUpdateRequest(interestIds);
            given(tagRepository.findAllById(interestIds)).willReturn(List.of(mock(Tag.class)));

            // when & then
            assertThatThrownBy(() -> memberService.updateMemberInterests(AUTH_MEMBER, request))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.TAG_NOT_FOUND));
        }
    }
}
