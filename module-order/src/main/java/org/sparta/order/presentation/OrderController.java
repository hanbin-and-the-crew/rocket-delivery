package org.sparta.order.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.sparta.common.api.ApiResponse;
import org.sparta.order.application.dto.request.OrderRequest;
import org.sparta.order.application.dto.response.OrderResponse;
import org.sparta.order.application.dto.response.OrderSearchCondition;
import org.sparta.order.application.service.OrderService;
import org.sparta.order.domain.enumeration.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 주문 컨트롤러
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController implements OrderApiSpec {

    private final OrderService orderService;

    @Override   // 주문 생성
    @PostMapping
    public ApiResponse<OrderResponse.Create> createOrder(
            @Valid @RequestBody OrderRequest.Create request,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        OrderResponse.Create response = orderService.createOrder(request, userId);
        return ApiResponse.success(response);
    }

    @Override   // 주문 단건 조회
    @GetMapping("/{orderId}")
    public ApiResponse<OrderResponse.Detail> getOrder(
            @PathVariable UUID orderId,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        OrderResponse.Detail response = orderService.getOrder(orderId, userId);
        return ApiResponse.success(response);
    }

    /**
     * 주문 목록 조회 (페이징)
     */
    @Override
    @GetMapping
    public ApiResponse<Page<OrderResponse.Summary>> searchOrders(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        Page<OrderResponse.Summary> result = orderService.searchOrders(pageable, userId);
        return ApiResponse.success(result);
    }


    @Override   // 배송 기한 변경
    @PatchMapping("/{orderId}/due-at")
    public ApiResponse<OrderResponse.Update> changeDueAt(
            @PathVariable UUID orderId,
            @Valid @RequestBody OrderRequest.ChangeDueAt request,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        OrderResponse.Update response = orderService.changeDueAt(orderId, request, userId);
        return ApiResponse.success(response);
    }

    @Override   // 요청사항 변경
    @PatchMapping("/{orderId}/memo")
    public ApiResponse<OrderResponse.Update> changeMemo(
            @PathVariable UUID orderId,
            @Valid @RequestBody OrderRequest.ChangeMemo request,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        OrderResponse.Update response = orderService.changeMemo(orderId, request, userId);
        return ApiResponse.success(response);
    }

    // TODO: 구현 예정
    @Override   // 요청사항 변경
    @PatchMapping("/{orderId}/address")
    public ApiResponse<OrderResponse.Update> changeAddress(
            @PathVariable UUID orderId,
            @Valid @RequestBody OrderRequest.ChangeAddress request,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        OrderResponse.Update response = orderService.changeAddress(orderId, request, userId);
        return ApiResponse.success(response);
    }

    @Override  // 출고 완료 (상태 변경)
    @PostMapping("/{orderId}/dispatch")
    public ApiResponse<OrderResponse.Update> dispatchOrder(
            @PathVariable UUID orderId,
            @Valid @RequestBody OrderRequest.Dispatch request,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        OrderResponse.Update response = orderService.dispatchOrder(orderId, request, userId);
        return ApiResponse.success(response);
    }

    @Override   // 주문 취소
    @PostMapping("/{orderId}/cancel")
    public ApiResponse<OrderResponse.Update> cancelOrder(
            @PathVariable UUID orderId,
            @Valid @RequestBody OrderRequest.Cancel request,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        OrderResponse.Update response = orderService.cancelOrder(orderId, request, userId);
        return ApiResponse.success(response);
    }

    @Override // 주문 삭제
    @DeleteMapping("/{orderId}")
    public ApiResponse<Void> deleteOrder(   
            @PathVariable UUID orderId,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        orderService.deleteOrder(orderId, userId);
        return ApiResponse.success(null);
    }
}