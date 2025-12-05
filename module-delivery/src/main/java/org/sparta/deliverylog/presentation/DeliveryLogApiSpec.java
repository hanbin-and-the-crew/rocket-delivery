package org.sparta.deliverylog.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.sparta.common.api.ApiResponse;
import org.sparta.deliverylog.presentation.dto.request.DeliveryLogRequest;
import org.sparta.deliverylog.presentation.dto.response.DeliveryLogResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@Tag(name = "DeliveryLog API", description = "배송 로그 관리 API")
public interface DeliveryLogApiSpec {

    @Operation(
            summary = "배송 로그 생성",
            description = "관리자/테스트용으로 배송 로그를 직접 생성합니다."
    )
    ApiResponse<DeliveryLogResponse.Detail> create(
            @Valid @RequestBody DeliveryLogRequest.Create request
    );

    @Operation(
            summary = "배송 로그 단건 조회",
            description = "배송 로그 ID로 상세 정보를 조회합니다."
    )
    ApiResponse<DeliveryLogResponse.Detail> getDetail(
            @PathVariable UUID logId
    );

    @Operation(
            summary = "특정 배송의 로그 타임라인 조회",
            description = "deliveryId 기준으로 전체 로그를 sequence 오름차순으로 조회합니다."
    )
    ApiResponse<List<DeliveryLogResponse.Summary>> getTimelineByDeliveryId(
            @PathVariable UUID deliveryId
    );

    @Operation(
            summary = "배송 로그 검색",
            description = "허브 ID, 배송 담당자 ID, 배송 ID로 배송 로그를 검색합니다. createdAt 기준 정렬/페이징을 지원합니다."
    )
    ApiResponse<DeliveryLogResponse.PageResult> search(
            @RequestParam(required = false) UUID hubId,
            @RequestParam(required = false) UUID deliveryManId,
            @RequestParam(required = false) UUID deliveryId,
            @RequestParam(required = false, defaultValue = "ASC") String sortDirection,
            @PageableDefault(size = 10) Pageable pageable
    );

    @Operation(
            summary = "배송 로그 담당자 배정",
            description = "배송 로그에 허브 배송 담당자를 배정합니다. (CREATED → HUB_WAITING)"
    )
    ApiResponse<DeliveryLogResponse.Detail> assignDeliveryMan(
            @PathVariable UUID logId,
            @Valid @RequestBody DeliveryLogRequest.AssignDeliveryMan request
    );

    @Operation(
            summary = "허브 leg 출발 처리",
            description = "해당 배송 로그를 허브 출발 상태로 변경합니다. (HUB_WAITING → HUB_MOVING)"
    )
    ApiResponse<DeliveryLogResponse.Detail> startLog(
            @PathVariable UUID logId
    );

    @Operation(
            summary = "허브 leg 도착 처리",
            description = "해당 배송 로그를 허브 도착 상태로 변경하고, 실제 거리/시간을 기록합니다. (HUB_MOVING → HUB_ARRIVED)"
    )
    ApiResponse<DeliveryLogResponse.Detail> arriveLog(
            @PathVariable UUID logId,
            @Valid @RequestBody DeliveryLogRequest.Arrive request
    );

    @Operation(
            summary = "배송 취소에 따른 로그 취소",
            description = "배송 또는 주문 취소에 따라 배송 로그를 취소 상태로 변경합니다. (CREATED/HUB_WAITING → CANCELED)"
    )
    ApiResponse<DeliveryLogResponse.Detail> cancelFromDelivery(
            @PathVariable UUID logId
    );

    @Operation(
            summary = "배송 로그 삭제",
            description = "배송 로그를 소프트 삭제합니다."
    )
    ApiResponse<Void> delete(
            @PathVariable UUID logId
    );
}
