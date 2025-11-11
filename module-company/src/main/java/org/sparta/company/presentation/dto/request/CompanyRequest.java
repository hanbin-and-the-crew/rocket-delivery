package org.sparta.company.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.sparta.company.domain.model.CompanyType;

import java.util.UUID;

public record CompanyRequest(
        @NotBlank(message = "업체명은 필수입니다.")
        String name,
        @NotNull(message = "업체 타입은 필수입니다.")
        CompanyType type,
        @NotNull(message = "허브 ID는 필수입니다.")
        UUID hubId,
        @NotBlank(message = "주소는 필수입니다.")
        String address
) {}
