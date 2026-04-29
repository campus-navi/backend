package com.campusnavi.backend.official.post.dto;

import com.campusnavi.backend.member.dto.MemberScope;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OfficialPostScopeConditionTest {

    @Nested
    @DisplayName("스코프 조건 생성")
    class From {

        @Test
        @DisplayName("스코프가 비어 있으면 universityId만 포함하고 나머지는 null이다")
        void emptyScopes() {
            OfficialPostScopeCondition condition = OfficialPostScopeCondition.from(10L, List.of());

            assertThat(condition.universityId()).isEqualTo(10L);
            assertThat(condition.campusId()).isNull();
            assertThat(condition.collegeIds()).isNull();
            assertThat(condition.departmentIds()).isNull();
        }

        @Test
        @DisplayName("스코프가 있으면 campusId와 전체 collegeId, departmentId 목록을 포함한다")
        void withMultipleScopes() {
            List<MemberScope> scopes = List.of(
                    new MemberScope(1L, 10L, 100L),
                    new MemberScope(1L, 20L, 200L)
            );

            OfficialPostScopeCondition condition = OfficialPostScopeCondition.from(5L, scopes);

            assertThat(condition.universityId()).isEqualTo(5L);
            assertThat(condition.campusId()).isEqualTo(1L);
            assertThat(condition.collegeIds()).containsExactly(10L, 20L);
            assertThat(condition.departmentIds()).containsExactly(100L, 200L);
        }

        @Test
        @DisplayName("collegeId나 departmentId가 null인 스코프는 각 목록에서 제외된다")
        void nullFieldsFiltered() {
            List<MemberScope> scopes = List.of(
                    new MemberScope(1L, null, null),
                    new MemberScope(1L, 20L, 200L)
            );

            OfficialPostScopeCondition condition = OfficialPostScopeCondition.from(5L, scopes);

            assertThat(condition.collegeIds()).containsExactly(20L);
            assertThat(condition.departmentIds()).containsExactly(200L);
        }

        @Test
        @DisplayName("모든 스코프의 collegeId, departmentId가 null이면 빈 목록이 된다")
        void allNullFields() {
            List<MemberScope> scopes = List.of(
                    new MemberScope(1L, null, null)
            );

            OfficialPostScopeCondition condition = OfficialPostScopeCondition.from(5L, scopes);

            assertThat(condition.campusId()).isEqualTo(1L);
            assertThat(condition.collegeIds()).isEmpty();
            assertThat(condition.departmentIds()).isEmpty();
        }
    }
}
