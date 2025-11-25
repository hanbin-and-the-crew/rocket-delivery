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
     * Create a new order for the authenticated customer.
     *
     * @param userIdHeader the value of the `X-USER-ID` request header (UUID string identifying the customer)
     * @param request the order creation payload
     * @return the created order details wrapped in an API response
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
     * Retrieve a single order by its identifier.
     *
     * @param orderId the UUID of the order
     * @return the detailed representation of the order
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
     * Retrieve the customer's orders identified by the X-USER-ID header as a pageable list.
     *
     * @param userIdHeader the UUID string from the X-USER-ID request header identifying the customer
     * @param pageable     paging and sorting parameters (e.g., page, size — 10/30/50, sort)
     * @return             a page of OrderResponse.Summary representing the customer's orders
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
     * Change the due date of the specified order.
     *
     * @param orderId the UUID of the order to update
     * @param request the change request containing the new due date
     * @return an OrderResponse.Update containing the order's updated information after the due date change
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
     * Change the delivery address for the specified order.
     *
     * @param orderId the UUID of the order to update
     * @param request DTO containing the new delivery address details
     * @return the updated order information as an OrderResponse.Update wrapped in ApiResponse
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
         * Updates the customer request memo for the specified order.
         *
         * @param orderId the UUID of the order to update
         * @param request the request containing the new memo text
         * @return an OrderResponse.Update describing the order after the memo change
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
     * Cancel an order and return the updated order information.
     *
     * The order identifier provided in the path takes precedence over any orderId present in the request body.
     *
     * @param orderId the order ID from the request path (used as the authoritative order identifier)
     * @param request cancellation details (reason code and memo); any orderId inside this DTO is ignored
     * @return the updated order information after cancellation
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
     * Marks the specified order as shipped and initiates its delivery.
     *
     * @return the updated order information reflecting the shipment state
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
     * Mark an order as delivered.
     *
     * @param orderId the UUID of the order to mark as delivered
     * @return an OrderResponse.Update containing the updated order state after delivery
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
     * Logically delete the order identified by the given ID.
     *
     * @param orderId the UUID of the order to delete
     * @return the update response containing the order's state after the logical deletion
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