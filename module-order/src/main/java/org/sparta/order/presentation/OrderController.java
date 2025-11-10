package org.sparta.order.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.sparta.common.api.ApiResponse;
import org.sparta.order.application.dto.request.OrderRequest;
import org.sparta.order.application.dto.response.OrderResponse;
import org.sparta.order.application.service.OrderService;
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

    @Override
    @PostMapping
    public ApiResponse<OrderResponse.Create> createOrder(
            @Valid @RequestBody OrderRequest.Create request,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        OrderResponse.Create response = orderService.createOrder(request, userId);
        return ApiResponse.success(response);
    }

    @Override
    @GetMapping("/{orderId}")
    public ApiResponse<OrderResponse.Detail> getOrder(
            @PathVariable UUID orderId
    ) {
        OrderResponse.Detail response = orderService.getOrder(orderId);
        return ApiResponse.success(response);
    }

//    @Override
//    @GetMapping
//    public ApiResponse<Page<OrderResponse.Summary>> searchOrders(
//            @RequestParam(required = false) UUID supplierId,
//            @RequestParam(required = false) UUID receiptCompanyId,
//            @RequestParam(required = false) UUID productId,
//            @RequestParam(required = false) String status,
//            Pageable pageable
//    ) {
//        OrderSearchCondition condition = OrderSearchCondition.of(
//                supplierId,
//                receiptCompanyId,
//                productId,
//                status != null ? OrderStatus.valueOf(status) : null,
//                null,
//                null,
//                null,
//                null
//        );
//
//        Page<OrderResponse.Summary> response = orderService.searchOrders(condition, pageable);
//        return ApiResponse.success(response);
//    }

    @Override
    @PatchMapping("/{orderId}/quantity")
    public ApiResponse<OrderResponse.Update> changeQuantity(
            @PathVariable UUID orderId,
            @Valid @RequestBody OrderRequest.ChangeQuantity request,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        OrderResponse.Update response = orderService.changeQuantity(orderId, request, userId);
        return ApiResponse.success(response);
    }

    @Override
    @PatchMapping("/{orderId}/due-at")
    public ApiResponse<OrderResponse.Update> changeDueAt(
            @PathVariable UUID orderId,
            @Valid @RequestBody OrderRequest.ChangeDueAt request,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        OrderResponse.Update response = orderService.changeDueAt(orderId, request, userId);
        return ApiResponse.success(response);
    }

    @Override
    @PatchMapping("/{orderId}/memo")
    public ApiResponse<OrderResponse.Update> changeMemo(
            @PathVariable UUID orderId,
            @Valid @RequestBody OrderRequest.ChangeMemo request,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        OrderResponse.Update response = orderService.changeMemo(orderId, request, userId);
        return ApiResponse.success(response);
    }

    @Override
    @PostMapping("/{orderId}/dispatch")
    public ApiResponse<OrderResponse.Update> dispatchOrder(
            @PathVariable UUID orderId,
            @Valid @RequestBody OrderRequest.Dispatch request,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        OrderResponse.Update response = orderService.dispatchOrder(orderId, request, userId);
        return ApiResponse.success(response);
    }

    @Override
    @PostMapping("/{orderId}/cancel")
    public ApiResponse<OrderResponse.Update> cancelOrder(
            @PathVariable UUID orderId,
            @Valid @RequestBody OrderRequest.Cancel request,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        OrderResponse.Update response = orderService.cancelOrder(orderId, request, userId);
        return ApiResponse.success(response);
    }

    @Override
    @DeleteMapping("/{orderId}")
    public ApiResponse<Void> deleteOrder(
            @PathVariable UUID orderId,
            @RequestHeader("X-User-Id") String deletedBy
    ) {
        orderService.deleteOrder(orderId, deletedBy);
        return ApiResponse.success(null);
    }
}