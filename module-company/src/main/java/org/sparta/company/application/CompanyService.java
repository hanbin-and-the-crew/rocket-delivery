package org.sparta.company.application;

import lombok.RequiredArgsConstructor;
import org.sparta.company.domain.entity.Company;
import org.sparta.company.domain.repository.CompanyRepository;
import org.sparta.company.exception.AlreadyDeletedCompanyException;
import org.sparta.company.exception.CompanyNotFoundException;
import org.sparta.company.exception.DuplicateCompanyNameException;
import org.sparta.company.presentation.dto.request.CompanyRequest;
import org.sparta.company.presentation.dto.response.CompanyResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompanyService {

    private final CompanyRepository companyRepository;

    @Transactional
    public Company createCompany(CompanyRequest request) {
        if (companyRepository.existsByName(request.name())) {
            throw new DuplicateCompanyNameException(request.name());
        }
        Company company = Company.create(
                request.name(),
                request.type(),
                request.hubId(),
                request.address()
        );
        return companyRepository.save(company);
    }

    public Company getCompany(UUID id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new CompanyNotFoundException(id));
    }


    public List<Company> getAllActive() {
        return companyRepository.findAllActive();
    }


    @Transactional
    public Company updateCompany(UUID id, CompanyRequest request) {
        Company company = getCompany(id);
        if (!company.getName().equals(request.name())
                && companyRepository.existsByName(request.name())) {
            throw new DuplicateCompanyNameException(request.name());
        }
        company.update(request.name(), request.type(), request.address());
        return companyRepository.save(company);
    }


    @Transactional
    public void deleteCompany(UUID id) {
        Company company = getCompany(id);
        if (!company.isActive()) throw new AlreadyDeletedCompanyException();
        company.markAsDeleted();
        companyRepository.save(company);
    }
}
