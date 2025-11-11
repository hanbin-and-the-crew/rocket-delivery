package org.sparta.company.domain.repository;

import org.sparta.company.domain.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface CompanyRepository extends JpaRepository<Company, UUID> {

    boolean existsByName(String name);

    @Query("SELECT c FROM Company c WHERE c.active = true")
    List<Company> findAllActive();
}
