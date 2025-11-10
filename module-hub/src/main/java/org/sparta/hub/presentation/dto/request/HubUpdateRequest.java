package org.sparta.hub.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.sparta.hub.domain.model.HubStatus;

/**
 * 허브 수정 요청 DTO
 * PathVariable로 hubId를 전달하므로 hubId 필드는 제거됨.
 */
public record HubUpdateRequest(
        @NotBlank(message = "허브 이름은 필수입니다.")
        String name,
        @NotBlank(message = "주소는 필수입니다.")
        String address,
        @NotNull(message = "위도는 필수입니다.")
        Double latitude,
        @NotNull(message = "경도는 필수입니다.")
        Double longitude,
        @NotNull(message = "상태는 필수입니다.")
        HubStatus status
) { }
