package org.sparta.delivery.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.sparta.common.api.ApiResponse;
import org.sparta.delivery.infrastructure.event.OrderApprovedEvent;
import org.sparta.delivery.presentation.dto.request.DeliveryRequest;
import org.sparta.delivery.presentation.dto.response.DeliveryResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@Tag(name = "Delivery API", description = "배송 관리 API")
public interface DeliveryApiSpec {

    @Operation(
            summary = "배송 생성(단순)",
            description = "컨트롤러에서 받은 값만으로 Delivery만 생성합니다. 허브 경로/로그는 생성하지 않습니다."
    )
    ApiResponse<DeliveryResponse.Detail> createSimple(
            @Valid @RequestBody DeliveryRequest.Create request
    );

    @Operation(
            summary = "배송 생성(허브 경로 + 로그 포함)",
            description = "허브 서비스에서 경로를 조회하고, 각 leg에 대한 DeliveryLog까지 함께 생성합니다."
    )
    ApiResponse<DeliveryResponse.Detail> createWithRoute(
            @Valid @RequestBody OrderApprovedEvent event
    );

    @Operation(
            summary = "배송 단건 조회",
            description = "배송 ID로 상세 정보를 조회합니다."
    )
    ApiResponse<DeliveryResponse.Detail> getDetail(
            @PathVariable UUID deliveryId
    );

    @Operation(
            summary = "배송 검색",
            description = "상태, 허브 ID, 업체 ID 기준으로 배송을 검색합니다. createdAt 기준 정렬/페이징을 지원합니다."
    )
    ApiResponse<DeliveryResponse.PageResult> search(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID hubId,
            @RequestParam(required = false) UUID companyId,
            @RequestParam(required = false, defaultValue = "ASC") String sortDirection,
            @PageableDefault(size = 10) Pageable pageable
    );

    @Operation(
            summary = "허브 배송 담당자 배정",
            description = "배송에 허브 배송 담당자를 배정합니다. (CREATED/HUB_WAITING 상태에서만 가능)"
    )
    ApiResponse<DeliveryResponse.Detail> assignHubDeliveryMan(
            @PathVariable UUID deliveryId,
            @Valid @RequestBody DeliveryRequest.AssignHubDeliveryMan request
    );

    @Operation(
            summary = "업체 배송 담당자 배정",
            description = "배송에 업체 배송 담당자를 배정합니다. (DEST_HUB_ARRIVED/COMPANY_MOVING 상태에서 가능)"
    )
    ApiResponse<DeliveryResponse.Detail> assignCompanyDeliveryMan(
            @PathVariable UUID deliveryId,
            @Valid @RequestBody DeliveryRequest.AssignCompanyDeliveryMan request
    );

    @Operation(
            summary = "허브 leg 출발 처리",
            description = "해당 시퀀스의 허브 leg 출발을 처리합니다. (HUB_WAITING → HUB_MOVING)"
    )
    ApiResponse<DeliveryResponse.Detail> startHubMoving(
            @PathVariable UUID deliveryId,
            @Valid @RequestBody DeliveryRequest.StartHubMoving request
    );

    @Operation(
            summary = "허브 leg 도착 처리",
            description = "해당 시퀀스의 허브 leg 도착을 처리하고, 실제 거리/시간을 기록합니다."
    )
    ApiResponse<DeliveryResponse.Detail> completeHubMoving(
            @PathVariable UUID deliveryId,
            @Valid @RequestBody DeliveryRequest.CompleteHubMoving request
    );

    @Operation(
            summary = "업체 배송 시작",
            description = "허브 구간이 모두 끝난 뒤 업체로 이동을 시작합니다. (DEST_HUB_ARRIVED → COMPANY_MOVING)"
    )
    ApiResponse<DeliveryResponse.Detail> startCompanyMoving(
            @PathVariable UUID deliveryId
    );

    @Operation(
            summary = "최종 배송 완료",
            description = "업체 배송을 완료 처리합니다. (COMPANY_MOVING → DELIVERED)"
    )
    ApiResponse<DeliveryResponse.Detail> completeDelivery(
            @PathVariable UUID deliveryId
    );

    @Operation(
            summary = "배송 취소",
            description = "CREATED/HUB_WAITING 상태의 배송을 취소합니다."
    )
    ApiResponse<DeliveryResponse.Detail> cancel(
            @PathVariable UUID deliveryId
    );

    @Operation(
            summary = "배송 삭제",
            description = "배송을 소프트 삭제합니다."
    )
    ApiResponse<Void> delete(
            @PathVariable UUID deliveryId
    );
}
