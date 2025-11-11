package org.sparta.company.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sparta.company.domain.entity.Company;
import org.sparta.company.domain.model.CompanyType;
import org.sparta.company.domain.repository.CompanyRepository;
import org.sparta.company.exception.AlreadyDeletedCompanyException;
import org.sparta.company.exception.CompanyNotFoundException;
import org.sparta.company.exception.DuplicateCompanyNameException;
import org.sparta.company.presentation.dto.request.CompanyRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CompanyService 단위 테스트")
class CompanyServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    @InjectMocks
    private CompanyService companyService;

    @Test
    @DisplayName("createCompany - 신규 업체 생성 성공")
    void createCompany_success() {
        // given
        UUID hubId = UUID.randomUUID();
        CompanyRequest req = new CompanyRequest("스파르타식품", CompanyType.SUPPLIER, hubId, "서울특별시 강남구");
        Company mock = Company.create(req.name(), req.type(), req.hubId(), req.address());

        given(companyRepository.existsByName(req.name())).willReturn(false);
        given(companyRepository.save(any(Company.class))).willReturn(mock);

        // when
        Company result = companyService.createCompany(req);

        // then
        assertThat(result.getName()).isEqualTo("스파르타식품");
        then(companyRepository).should().save(any(Company.class));
    }

    @Test
    @DisplayName("createCompany - 중복된 업체명일 경우 예외 발생")
    void createCompany_duplicateName_throwsException() {
        // given
        UUID hubId = UUID.randomUUID();
        CompanyRequest req = new CompanyRequest("중복업체", CompanyType.SUPPLIER, hubId, "서울특별시");
        given(companyRepository.existsByName(req.name())).willReturn(true);

        // when & then
        assertThatThrownBy(() -> companyService.createCompany(req))
                .isInstanceOf(DuplicateCompanyNameException.class)
                .hasMessageContaining("Duplicate company name");
    }

    @Test
    @DisplayName("getCompany - 존재하지 않는 업체 조회 시 예외 발생")
    void getCompany_notFound_throwsException() {
        // given
        UUID id = UUID.randomUUID();
        given(companyRepository.findById(id)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> companyService.getCompany(id))
                .isInstanceOf(CompanyNotFoundException.class);
    }

    @Test
    @DisplayName("updateCompany - 이름이 중복일 경우 예외 발생")
    void updateCompany_duplicateName_throwsException() {
        // given
        UUID id = UUID.randomUUID();
        Company company = Company.create("스파르타", CompanyType.SUPPLIER, UUID.randomUUID(), "서울시");
        CompanyRequest req = new CompanyRequest("기존중복", CompanyType.RECEIVER, UUID.randomUUID(), "부산시");

        given(companyRepository.findById(id)).willReturn(Optional.of(company));
        given(companyRepository.existsByName(req.name())).willReturn(true);

        // when & then
        assertThatThrownBy(() -> companyService.updateCompany(id, req))
                .isInstanceOf(DuplicateCompanyNameException.class);
    }

    @Test
    @DisplayName("updateCompany - 정상 수정 성공")
    void updateCompany_success() {
        // given
        UUID id = UUID.randomUUID();
        Company company = Company.create("스파르타", CompanyType.SUPPLIER, UUID.randomUUID(), "서울시");
        CompanyRequest req = new CompanyRequest("새이름", CompanyType.RECEIVER, UUID.randomUUID(), "대전시");

        given(companyRepository.findById(id)).willReturn(Optional.of(company));
        given(companyRepository.existsByName(req.name())).willReturn(false);
        given(companyRepository.save(any(Company.class))).willReturn(company);

        // when
        Company updated = companyService.updateCompany(id, req);

        // then
        assertThat(updated.getName()).isEqualTo("새이름");
        then(companyRepository).should().save(any(Company.class));
    }

    @Test
    @DisplayName("deleteCompany - 이미 비활성화된 업체 삭제 시 예외 발생")
    void deleteCompany_alreadyDeleted_throwsException() {
        // given
        UUID id = UUID.randomUUID();
        Company deleted = Company.create("삭제테스트", CompanyType.RECEIVER, UUID.randomUUID(), "서울시");
        deleted.markAsDeleted();

        given(companyRepository.findById(id)).willReturn(Optional.of(deleted));

        // when & then
        assertThatThrownBy(() -> companyService.deleteCompany(id))
                .isInstanceOf(AlreadyDeletedCompanyException.class);
    }

    @Test
    @DisplayName("deleteCompany - 정상 삭제 시 active=false로 변경")
    void deleteCompany_success() {
        // given
        UUID id = UUID.randomUUID();
        Company company = Company.create("삭제대상", CompanyType.SUPPLIER, UUID.randomUUID(), "서울특별시");

        given(companyRepository.findById(id)).willReturn(Optional.of(company));
        given(companyRepository.save(any())).willReturn(company);

        // when
        companyService.deleteCompany(id);

        // then
        assertThat(company.isActive()).isFalse();
        then(companyRepository).should().save(any(Company.class));
    }

    @Test
    @DisplayName("getAllActive - 활성화된 업체 목록 반환")
    void getAllActive_success() {
        // given
        List<Company> mockList = List.of(
                Company.create("A업체", CompanyType.SUPPLIER, UUID.randomUUID(), "서울"),
                Company.create("B업체", CompanyType.RECEIVER, UUID.randomUUID(), "부산")
        );
        given(companyRepository.findAllActive()).willReturn(mockList);

        // when
        List<Company> result = companyService.getAllActive();

        // then
        assertThat(result).hasSize(2);
        then(companyRepository).should().findAllActive();
    }
}
