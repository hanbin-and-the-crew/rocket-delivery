package org.sparta.deliveryman.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.sparta.common.api.ApiResponse;
import org.sparta.deliveryman.domain.enumeration.DeliveryManStatus;
import org.sparta.deliveryman.domain.enumeration.DeliveryManType;
import org.sparta.deliveryman.presentation.dto.request.DeliveryManRequest;
import org.sparta.deliveryman.presentation.dto.response.DeliveryManResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;


import java.util.List;
import java.util.UUID;

@Tag(name = "DeliveryMan API", description = "배달원 관리 API")
public interface DeliveryManApiSpec {

    @Operation(
            summary = "배달원 생성",
            description = "관리자가 수동으로 배달원을 생성합니다."
    )
    ApiResponse<DeliveryManResponse.Detail> createDeliveryMan(
            @Valid @RequestBody DeliveryManRequest.Create request
    );

    @Operation(
            summary = "배달원 상태 변경",
            description = "배달원의 상태를 변경합니다. (WAITING/DELIVERING/OFFLINE/DELETED)"
    )
    ApiResponse<DeliveryManResponse.Detail> updateStatus(
            @PathVariable UUID deliveryManId,
            @Valid @RequestBody DeliveryManRequest.UpdateStatus request
    );

    @Operation(
            summary = "배달원 단건 조회",
            description = "배달원 ID로 배달원 상세 정보를 조회합니다."
    )
    ApiResponse<DeliveryManResponse.Detail> getDetail(
            @PathVariable UUID deliveryManId
    );

    @Operation(
            summary = "배달원 목록 조회",
            description = "허브, 타입, 상태, 실명 조건으로 배달원 목록을 조회합니다."
    )
    ApiResponse<List<DeliveryManResponse.Summary>> search(
            @RequestParam(required = false) UUID hubId,
            @RequestParam(required = false) DeliveryManType type,
            @RequestParam(required = false) DeliveryManStatus status,
            @RequestParam(required = false) String realName
    );

    @Operation(
            summary = "테스트용 - 허브 담당자 배정",
            description = "테스트를 위해 특정 허브에 배달원을 배정합니다."
    )
    ApiResponse<DeliveryManResponse.AssignResult> assignHubDeliveryMan(
    );

    @Operation(
            summary = "테스트용 - 업체 담당자 배정",
            description = "테스트를 위해 허브 내 특정 업체에 배달원을 배정합니다."
    )
    ApiResponse<DeliveryManResponse.AssignResult> assignCompanyDeliveryMan(
            @RequestParam UUID hubId
    );
}
