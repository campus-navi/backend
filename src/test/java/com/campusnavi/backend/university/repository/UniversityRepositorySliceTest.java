package com.campusnavi.backend.university.repository;

import com.campusnavi.backend.support.PostgresSliceTestSupport;
import com.campusnavi.backend.university.entity.University;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class UniversityRepositorySliceTest extends PostgresSliceTestSupport {

    @Autowired
    private UniversityRepository universityRepository;

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("University를 저장하면 ID가 부여된다")
        void saveAssignsId() {
            //given
            University university = University.create("테스트대학교");

            //when
            University saved = universityRepository.save(university);

            //then
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getName()).isEqualTo("테스트대학교");
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("저장한 University를 ID로 조회할 수 있다")
        void findByIdReturnsSavedUniversity() {
            //given
            University saved = universityRepository.save(University.create("테스트대학교"));

            //when
            Optional<University> found = universityRepository.findById(saved.getId());

            //then
            assertThat(found).isPresent();
            assertThat(found.get().getName()).isEqualTo("테스트대학교");
        }

        @Test
        @DisplayName("존재하지 않는 ID는 Optional.empty를 반환한다")
        void findByIdReturnsEmptyForMissingId() {
            //when
            Optional<University> found = universityRepository.findById(99999L);

            //then
            assertThat(found).isEmpty();
        }
    }
}
