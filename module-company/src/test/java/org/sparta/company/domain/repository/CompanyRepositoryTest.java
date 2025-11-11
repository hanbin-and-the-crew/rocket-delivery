package org.sparta.company.domain.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sparta.company.domain.entity.Company;
import org.sparta.company.domain.model.CompanyType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class CompanyRepositoryTest {

    @Autowired
    private CompanyRepository companyRepository;

    @Test
    @DisplayName("업체 저장 및 조회 성공")
    void saveAndFind_success() {
        // given
        Company company = Company.create("스파르타식품", CompanyType.SUPPLIER, "서울특별시 강남구");

        // when
        Company saved = companyRepository.save(company);
        Optional<Company> found = companyRepository.findById(saved.getCompanyId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("스파르타식품");
        assertThat(found.get().getType()).isEqualTo(CompanyType.SUPPLIER);
        assertThat(found.get().getAddress()).isEqualTo("서울특별시 강남구");
        assertThat(found.get().isActive()).isTrue();
    }

    @Test
    @DisplayName("Soft Delete 후 findAllActive() 결과에서 제외된다")
    void findAllActive_excludesDeleted() {
        // given
        Company active = Company.create("활성업체", CompanyType.SUPPLIER, "서울시 송파구");
        Company deleted = Company.create("비활성업체", CompanyType.RECEIVER, "부산광역시");
        deleted.markAsDeleted("tester");

        companyRepository.saveAll(List.of(active, deleted));

        // when
        List<Company> list = companyRepository.findAllActive();

        // then
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getName()).isEqualTo("활성업체");
    }

    @Test
    @DisplayName("업체명 중복 여부 existsByName() 테스트")
    void existsByName_success() {
        // given
        Company company = Company.create("중복테스트업체", CompanyType.SUPPLIER, "서울특별시");
        companyRepository.save(company);

        // when
        boolean exists = companyRepository.existsByName("중복테스트업체");
        boolean notExists = companyRepository.existsByName("없는업체");

        // then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("삭제된 업체는 active=false 상태로 남고 deletedAt이 기록된다")
    void markAsDeleted_persistsInactive() {
        // given
        Company company = Company.create("삭제테스트", CompanyType.RECEIVER, "대전광역시");

        // when
        company.markAsDeleted("tester");
        Company saved = companyRepository.save(company);

        // then
        Company found = companyRepository.findById(saved.getCompanyId()).orElseThrow();
        assertThat(found.isActive()).isFalse();
        assertThat(found.getDeletedAt()).isNotNull();
        assertThat(found.getDeletedBy()).isEqualTo("tester");
    }
}
