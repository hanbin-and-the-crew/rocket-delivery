package org.sparta.order.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.sparta.common.api.ApiResponse;
import org.sparta.order.application.dto.request.OrderRequest;
import org.sparta.order.application.dto.response.OrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

/**
 * 주문 API 명세
 */
@Tag(name = "Order API", description = "주문 관리")
public interface OrderApiSpec {

    @Operation(
            summary = "주문 생성",
            description = "새로운 주문을 생성합니다"
    )
    ApiResponse<OrderResponse.Create> createOrder(
            @Valid @RequestBody OrderRequest.Create request,
            @RequestHeader("X-User-Id") UUID userId
    );

    @Operation(
            summary = "주문 조회",
            description = "주문 ID로 주문 상세 정보를 조회합니다"
    )
    ApiResponse<OrderResponse.Detail> getOrder(
            @PathVariable UUID orderId,
            @RequestHeader("X-User-Id") UUID userId
    );

//    @Operation(
//            summary = "주문 목록 조회",
//            description = "주문 목록을 검색 조건에 따라 조회합니다"
//    )
//    ApiResponse<Page<OrderResponse.Summary>> searchOrders(
//            @RequestParam(required = false) UUID supplierId,
//            @RequestParam(required = false) UUID receiptCompanyId,
//            @RequestParam(required = false) UUID productId,
//            @RequestParam(required = false) String status,
//            @RequestHeader("X-User-Id") UUID userId,
//            Pageable pageable
//    );

    @Operation(
            summary = "납품 기한 변경",
            description = "납품 기한을 변경합니다 (PLACED 상태에서만 가능)"
    )
    ApiResponse<OrderResponse.Update> changeDueAt(
            @PathVariable UUID orderId,
            @Valid @RequestBody OrderRequest.ChangeDueAt request,
            @RequestHeader("X-User-Id") UUID userId
    );

    @Operation(
            summary = "요청사항 변경",
            description = "요청사항을 변경합니다 (PLACED 상태에서만 가능)"
    )
    ApiResponse<OrderResponse.Update> changeMemo(
            @PathVariable UUID orderId,
            @Valid @RequestBody OrderRequest.ChangeMemo request,
            @RequestHeader("X-User-Id") UUID userId
    );

    @Operation(
            summary = "주소 변경",
            description = "주소를 변경합니다 (PLACED 상태에서만 가능)"
    )
    ApiResponse<OrderResponse.Update> changeAddress(
            @PathVariable UUID orderId,
            @Valid @RequestBody OrderRequest.ChangeAddress request,
            @RequestHeader("X-User-Id") UUID userId
    );

    @Operation(
            summary = "주문 출고 처리",
            description = "주문을 출고 처리합니다"
    )
    ApiResponse<OrderResponse.Update> dispatchOrder(
            @PathVariable UUID orderId,
            @Valid @RequestBody OrderRequest.Dispatch request,
            @RequestHeader("X-User-Id") UUID userId
    );

    @Operation(
            summary = "주문 취소",
            description = "주문을 취소합니다"
    )
    ApiResponse<OrderResponse.Update> cancelOrder(
            @PathVariable UUID orderId,
            @Valid @RequestBody OrderRequest.Cancel request,
            @RequestHeader("X-User-Id") UUID userId
    );

    @Operation(
            summary = "주문 삭제",
            description = "주문을 논리적으로 삭제합니다"
    )
    ApiResponse<Void> deleteOrder(
            @PathVariable UUID orderId,
            @RequestHeader("X-User-Id") UUID userId
    );
}