package org.sparta.delivery.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.sparta.common.api.ApiResponse;
import org.sparta.delivery.application.dto.DeliverySearchCondition;
import org.sparta.delivery.application.dto.request.DeliveryRequest;
import org.sparta.delivery.application.dto.response.DeliveryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;
import java.util.UUID;

@Tag(name = "Delivery", description = "배송 관리 API")
public interface DeliveryApiSpec {

    @Operation(summary = "배송 생성", description = "새로운 배송을 생성합니다")
    ApiResponse<DeliveryResponse.Create> createDelivery(
            @Valid DeliveryRequest.Create request,
            @Parameter(hidden = true) UUID userId
    );

    @Operation(summary = "배송 조회", description = "배송 상세 정보를 조회합니다")
    ApiResponse<DeliveryResponse.Detail> getDelivery(
            @Parameter(description = "배송 ID") UUID deliveryId,
            @Parameter(hidden = true) UUID userId
    );

//    @Operation(summary = "배송 목록 조회", description = "모든 배송 목록을 조회합니다")
//    ApiResponse<List<DeliveryResponse.Summary>> getAllDeliveries(
//            @Parameter(hidden = true) UUID userId
//    );

    @Operation(summary = "배송 목록 조회", description = "검색 조건과 페이징을 적용하여 배송 목록을 조회합니다.")
    ApiResponse<Page<DeliveryResponse.Summary>> getAllDeliveries(
            @RequestHeader("X-User-Id") UUID userId,
            @ModelAttribute DeliverySearchCondition condition,
            Pageable pageable
    );

    @Operation(summary = "배송지 주소 변경", description = "배송지 주소를 변경합니다")
    ApiResponse<DeliveryResponse.Detail> updateAddress(
            @Parameter(description = "배송 ID") UUID deliveryId,
            @Valid DeliveryRequest.UpdateAddress request,
            @Parameter(hidden = true) UUID userId
    );

    @Operation(summary = "배송 담당자 배정", description = "업체 배송 담당자와 허브 배송 담당자를 배정합니다")
    ApiResponse<DeliveryResponse.Detail> assignDeliveryMan(
            @Parameter(description = "배송 ID") UUID deliveryId,
            @Valid DeliveryRequest.AssignDeliveryMan request,
            @Parameter(hidden = true) UUID userId
    );

    @Operation(summary = "허브 대기", description = "배송을 허브 대기 상태로 변경합니다")
    ApiResponse<DeliveryResponse.Detail> hubWaiting(
            @Parameter(description = "배송 ID") UUID deliveryId,
            @Parameter(hidden = true) UUID userId
    );

    @Operation(summary = "허브 이동 시작", description = "배송의 허브 이동을 시작합니다")
    ApiResponse<DeliveryResponse.Detail> hubMoving(
            @Parameter(description = "배송 ID") UUID deliveryId,
            @Parameter(hidden = true) UUID userId
    );

    @Operation(summary = "목적지 허브 도착", description = "목적지 허브에 도착 처리합니다")
    ApiResponse<DeliveryResponse.Detail> arriveAtDestinationHub(
            @Parameter(description = "배송 ID") UUID deliveryId,
            @Parameter(hidden = true) UUID userId
    );

    @Operation(summary = "업체 이동 시작", description = "업체로의 이동을 시작합니다")
    ApiResponse<DeliveryResponse.Detail> startCompanyMoving(
            @Parameter(description = "배송 ID") UUID deliveryId,
            @Valid DeliveryRequest.StartCompanyMoving request,
            @Parameter(hidden = true) UUID userId
    );

    @Operation(summary = "배송 완료", description = "배송을 완료 처리합니다")
    ApiResponse<DeliveryResponse.Detail> completeDelivery(
            @Parameter(description = "배송 ID") UUID deliveryId,
            @Parameter(hidden = true) UUID userId
    );

    @Operation(summary = "배송 삭제", description = "배송을 삭제합니다")
    ApiResponse<Void> deleteDelivery(
            @Parameter(description = "배송 ID") UUID deliveryId,
            @Parameter(hidden = true) UUID userId
    );
}
