package org.sparta.hub.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 허브 생성 요청 DTO
 * Controller 입력 검증 전용
 */
public record HubCreateRequest(
        @NotBlank(message = "허브 이름은 필수입니다.")
        String name,

        @NotBlank(message = "주소는 필수입니다.")
        String address,

        @NotNull(message = "위도는 필수입니다.")
        Double latitude,

        @NotNull(message = "경도는 필수입니다.")
        Double longitude
) {}
