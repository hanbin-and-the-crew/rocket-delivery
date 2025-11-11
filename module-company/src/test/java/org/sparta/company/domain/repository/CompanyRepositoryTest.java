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
    @DisplayName("Company 저장 및 조회 성공")
    void saveAndFind_success() {
        // given
        UUID hubId = UUID.randomUUID();
        Company company = Company.create("스파르타로지스틱스", CompanyType.SUPPLIER, hubId, "서울특별시 강남구");

        // when
        Company saved = companyRepository.save(company);
        Optional<Company> found = companyRepository.findById(saved.getCompanyId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("스파르타로지스틱스");
        assertThat(found.get().getType()).isEqualTo(CompanyType.SUPPLIER);
        assertThat(found.get().getHubId()).isEqualTo(hubId);
        assertThat(found.get().isActive()).isTrue();
    }

    @Test
    @DisplayName("Soft Delete 후 findAllActive() 결과에서 제외된다")
    void findAllActive_excludesDeleted() {
        // given
        Company active = Company.create("활성업체", CompanyType.SUPPLIER, UUID.randomUUID(), "서울특별시 송파구");
        Company deleted = Company.create("비활성업체", CompanyType.RECEIVER, UUID.randomUUID(), "부산광역시");
        deleted.markAsDeleted();

        companyRepository.saveAll(List.of(active, deleted));

        // when
        List<Company> result = companyRepository.findAllActive();

        // then
        assertThat(result)
                .hasSize(1)
                .extracting(Company::getName)
                .containsExactly("활성업체");
    }

    @Test
    @DisplayName("existsByName()은 중복 이름이 존재하면 true 반환")
    void existsByName_success() {
        // given
        Company company = Company.create("중복테스트", CompanyType.SUPPLIER, UUID.randomUUID(), "서울특별시 마포구");
        companyRepository.save(company);

        // when
        boolean exists = companyRepository.existsByName("중복테스트");
        boolean notExists = companyRepository.existsByName("없는이름");

        // then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("markAsDeleted() 후 active=false, deletedAt 기록됨")
    void markAsDeleted_persistsInactive() {
        // given
        Company company = Company.create("삭제테스트", CompanyType.RECEIVER, UUID.randomUUID(), "대전광역시");

        // when
        company.markAsDeleted();
        companyRepository.save(company);

        // then
        Company found = companyRepository.findById(company.getCompanyId()).orElseThrow();
        assertThat(found.isActive()).isFalse();
        assertThat(found.getDeletedAt()).isNotNull();
    }
}
