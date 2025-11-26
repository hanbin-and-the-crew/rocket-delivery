package org.sparta.order.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.sparta.common.api.ApiResponse;
import org.sparta.order.application.dto.request.OrderRequest;
import org.sparta.order.application.dto.response.OrderResponse;
import org.sparta.order.application.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController implements OrderApiSpec {

    private final OrderService orderService;

    // TODO: 현재는 헤더로 X-USER-ID를 받아옴 => security가 공통 모듈에 추가 되면 바꿀 예정
    
    /**
     * 주문 생성
     */
    @Override
    @PostMapping
    public ApiResponse<OrderResponse.Detail> createOrder(
            @RequestHeader("X-USER-ID") String userIdHeader,
            @Valid @RequestBody OrderRequest.Create request
    ) {
        UUID customerId = UUID.fromString(userIdHeader);
        OrderResponse.Detail response = orderService.createOrder(customerId, request);
        return ApiResponse.success(response);
    }

    /**
     * 주문 단건 조회
     */
    @Override
    @GetMapping("/{orderId}")
    public ApiResponse<OrderResponse.Detail> getOrder(
            @PathVariable UUID orderId
    ) {
        OrderResponse.Detail response = orderService.getOrder(orderId);
        return ApiResponse.success(response);
    }

    /**
     * 내 주문 목록 조회
     * - X-USER-ID 헤더 기준
     * - page, size(10/30/50), sort 파라미터 사용
     */
    @Override
    @GetMapping
    public ApiResponse<Page<OrderResponse.Summary>> getMyOrders(
            @RequestHeader("X-USER-ID") String userIdHeader,
            Pageable pageable
    ) {
        UUID customerId = UUID.fromString(userIdHeader);
        Page<OrderResponse.Summary> response = orderService.getOrdersByCustomer(customerId, pageable);
        return ApiResponse.success(response);
    }

    /**
     * 주문 납기일 변경
     */
    @Override
    @PatchMapping("/{orderId}/due-at")
    public ApiResponse<OrderResponse.Update> changeDueAt(
            @PathVariable UUID orderId,
            @Valid @RequestBody OrderRequest.ChangeDueAt request
    ) {
        OrderResponse.Update response = orderService.changeDueAt(orderId, request);
        return ApiResponse.success(response);
    }

    /**
     * 주문 주소 변경
     */
    @Override
    @PatchMapping("/{orderId}/address")
    public ApiResponse<OrderResponse.Update> changeAddress(
            @PathVariable UUID orderId,
            @Valid @RequestBody OrderRequest.ChangeAddress request
    ) {
        OrderResponse.Update response = orderService.changeAddress(orderId, request);
        return ApiResponse.success(response);
    }

    /**
     * 주문 요청사항 변경
     */
    @Override
    @PatchMapping("/{orderId}/memo")
    public ApiResponse<OrderResponse.Update> changeRequestMemo(
            @PathVariable UUID orderId,
            @Valid @RequestBody OrderRequest.ChangeMemo request
    ) {
        OrderResponse.Update response = orderService.changeRequestMemo(orderId, request);
        return ApiResponse.success(response);
    }

    /**
     * 주문 취소
     * - path의 orderId와 body의 orderId가 다를 수 있어서,
     *   path 값을 우선시해서 새로운 Cancel DTO를 만들어 넘긴다.
     */
    @Override
    @PostMapping("/{orderId}/cancel")
    public ApiResponse<OrderResponse.Update> cancelOrder(
            @PathVariable UUID orderId,
            @Valid @RequestBody OrderRequest.Cancel request
    ) {
        OrderRequest.Cancel fixedRequest = new OrderRequest.Cancel(
                orderId,
                request.reasonCode(),
                request.reasonMemo()
        );
        OrderResponse.Update response = orderService.cancelOrder(fixedRequest);
        return ApiResponse.success(response);
    }

    /**
     * 주문 출고(배송 시작)
     */
    @Override
    @PostMapping("/{orderId}/ship")
    public ApiResponse<OrderResponse.Update> shipOrder(
            @PathVariable UUID orderId
    ) {
        OrderRequest.ShipOrder request = new OrderRequest.ShipOrder(orderId);
        OrderResponse.Update response = orderService.shipOrder(request);
        return ApiResponse.success(response);
    }

    /**
     * 주문 배송 완료 처리
     */
    @Override
    @PostMapping("/{orderId}/deliver")
    public ApiResponse<OrderResponse.Update> deliverOrder(
            @PathVariable UUID orderId
    ) {
        OrderRequest.DeliverOrder request = new OrderRequest.DeliverOrder(orderId);
        OrderResponse.Update response = orderService.deliverOrder(request);
        return ApiResponse.success(response);
    }

    /**
     * 주문 논리 삭제
     */
    @Override
    @DeleteMapping("/{orderId}")
    public ApiResponse<OrderResponse.Update> deleteOrder(
            @PathVariable UUID orderId
    ) {
        OrderRequest.DeleteOrder request = new OrderRequest.DeleteOrder(orderId);
        OrderResponse.Update response = orderService.deleteOrder(request);
        return ApiResponse.success(response);
    }
}
