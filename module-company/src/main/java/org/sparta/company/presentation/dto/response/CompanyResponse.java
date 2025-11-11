package org.sparta.company.presentation.dto.response;

import org.sparta.company.domain.entity.Company;

import java.time.LocalDateTime;
import java.util.UUID;

public record CompanyResponse(
        UUID companyId,
        String name,
        String type,
        UUID hubId,
        String address,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CompanyResponse from(Company c) {
        return new CompanyResponse(
                c.getCompanyId(),
                c.getName(),
                c.getType().name(),
                c.getHubId(),
                c.getAddress(),
                c.isActive(),
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }
}
