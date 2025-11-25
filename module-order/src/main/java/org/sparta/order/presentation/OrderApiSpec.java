package org.sparta.order.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.sparta.common.api.ApiResponse;
import org.sparta.order.application.dto.request.OrderRequest;
import org.sparta.order.application.dto.response.OrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Tag(name = "Order API", description = "주문 생성/조회/수정/취소/배송 처리 API")
public interface OrderApiSpec {

    /**
     * Create a new order using the X-USER-ID header value as the customer identifier.
     *
     * The created order is initialized with state CREATED. Subsequent inventory, payment,
     * and shipping processing are handled asynchronously by other services or events.
     *
     * @param userIdHeader UUID string taken from the `X-USER-ID` header representing the customer ID
     * @param request      order creation payload
     * @return             an ApiResponse containing the created order details as OrderResponse.Detail
     */
    @Operation(
            summary = "주문 생성",
            description = """
                    X-USER-ID 헤더의 사용자 ID를 customerId로 사용해서 주문을 생성합니다.
                    - 초기 상태: CREATED
                    - 이후 재고/결제/배송 프로세스는 이벤트/별도 서비스에서 처리
                    """,
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "주문 생성 성공",
                            content = @Content(
                                    schema = @Schema(implementation = OrderResponse.Detail.class)
                            )
                    )
            }
    )
    ApiResponse<OrderResponse.Detail> createOrder(
            @Parameter(
                    description = "주문자 사용자 ID (UUID 문자열)",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440010"
            )
            String userIdHeader,
            OrderRequest.Create request
    );

    /**
     * Retrieve detailed information for a specific order.
     *
     * @param orderId the UUID of the order to retrieve
     * @return the order detail wrapped in an ApiResponse
     */
    @Operation(
            summary = "주문 단건 조회",
            description = "주문 ID로 주문 상세 정보를 조회합니다."
    )
    ApiResponse<OrderResponse.Detail> getOrder(
            @Parameter(description = "주문 ID", required = true)
            java.util.UUID orderId
    );

    /**
     * Retrieve a paginated list of orders for the caller.
     *
     * Uses the X-USER-ID header value as the customer ID to filter orders. Supports
     * standard paging and sorting parameters (page, size, sort). Allowed page sizes:
     * 10, 30, 50.
     *
     * @param userIdHeader the caller's user ID as a UUID string (from the X-USER-ID header)
     * @param pageable pagination and sorting information
     * @return a page of OrderResponse.Summary objects for the specified user
     */
    @Operation(
            summary = "내 주문 목록 조회",
            description = """
                    X-USER-ID 헤더의 사용자 ID를 customerId로 사용하여
                    해당 사용자의 주문 목록을 페이징으로 조회합니다.
                    page, size(10/30/50), sort 파라미터 사용 가능
                    """
    )
    ApiResponse<Page<OrderResponse.Summary>> getMyOrders(
            @Parameter(
                    description = "주문자 사용자 ID (UUID 문자열)",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440010"
            )
            String userIdHeader,
            Pageable pageable
    );

    /**
     * Change the delivery due date of an order.
     *
     * Only orders in the CREATED state can have their due date changed.
     *
     * @param orderId the UUID of the order to modify
     * @param request the change request containing the new due date
     * @return an ApiResponse containing the updated order information (OrderResponse.Update)
     */
    @Operation(
            summary = "주문 납기일 변경",
            description = "CREATED 상태의 주문에 대해서만 납품 기한을 변경할 수 있습니다."
    )
    ApiResponse<OrderResponse.Update> changeDueAt(
            @Parameter(description = "주문 ID", required = true)
            java.util.UUID orderId,
            OrderRequest.ChangeDueAt request
    );

    /**
     * Change the shipping address for the specified order.
     *
     * This operation is permitted only when the order is in the CREATED state.
     *
     * @param orderId the UUID of the order to update
     * @param request the new shipping address information
     * @return an ApiResponse containing the updated order information after the address change
     */
    @Operation(
            summary = "주문 주소 변경",
            description = "CREATED 상태의 주문에 대해서만 배송지 주소를 변경할 수 있습니다."
    )
    ApiResponse<OrderResponse.Update> changeAddress(
            @Parameter(description = "주문 ID", required = true)
            java.util.UUID orderId,
            OrderRequest.ChangeAddress request
    );

    /**
     * Updates the customer's request memo for an order.
     *
     * Only orders in the CREATED state can have their memo updated.
     *
     * @param orderId the UUID of the order to update
     * @param request the memo change payload
     * @return an ApiResponse containing the updated order representation (OrderResponse.Update)
     */
    @Operation(
            summary = "주문 요청사항 변경",
            description = "CREATED 상태의 주문에 대해서만 요청사항(메모)을 변경할 수 있습니다."
    )
    ApiResponse<OrderResponse.Update> changeRequestMemo(
            @Parameter(description = "주문 ID", required = true)
            java.util.UUID orderId,
            OrderRequest.ChangeMemo request
    );

    /**
     * Cancel an order.
     *
     * Only orders in the CREATED or APPROVED state can be canceled. Cancellation requires a reason code and a detailed memo; orders in SHIPPED or DELIVERED state cannot be canceled.
     *
     * @param orderId the UUID of the order to cancel
     * @param request cancellation details, including required reason code and detailed memo
     * @return the updated order information after cancellation
     */
    @Operation(
            summary = "주문 취소",
            description = """
                    CREATED / APPROVED 상태의 주문만 취소할 수 있습니다.
                    - 취소 사유 코드 + 상세 메모 필수
                    - SHIPPED / DELIVERED 상태는 취소 불가
                    """
    )
    ApiResponse<OrderResponse.Update> cancelOrder(
            @Parameter(description = "주문 ID", required = true)
            java.util.UUID orderId,
            OrderRequest.Cancel request
    );

    /**
     * Mark an approved order as shipped.
     *
     * Transitions the order's status from APPROVED to SHIPPED and returns the updated order information.
     *
     * @param orderId the UUID of the order to mark as shipped
     * @return an ApiResponse containing an OrderResponse.Update reflecting the order after it transitions to SHIPPED
     */
    @Operation(
            summary = "주문 출고(배송 시작)",
            description = "APPROVED 상태의 주문을 SHIPPED 상태로 변경합니다."
    )
    ApiResponse<OrderResponse.Update> shipOrder(
            @Parameter(description = "주문 ID", required = true)
            java.util.UUID orderId
    );

    /**
     * Mark an order as delivered.
     *
     * Transitions an order that is currently in the SHIPPED state to the DELIVERED state.
     *
     * @param orderId the UUID of the order to mark as delivered
     * @return the updated order information with its state set to DELIVERED
     */
    @Operation(
            summary = "주문 배송 완료 처리",
            description = "SHIPPED 상태의 주문을 DELIVERED 상태로 변경합니다."
    )
    ApiResponse<OrderResponse.Update> deliverOrder(
            @Parameter(description = "주문 ID", required = true)
            java.util.UUID orderId
    );

    /**
     * Soft-delete the specified order.
     *
     * Only orders not in SHIPPED or DELIVERED states can be deleted; the order's `deletedAt` timestamp is set.
     *
     * @param orderId the ID of the order to soft-delete
     * @return the updated order information reflecting the deletion
     */
    @Operation(
            summary = "주문 논리 삭제",
            description = """
                    주문을 Soft Delete 처리합니다.
                    - SHIPPED / DELIVERED 상태의 주문은 삭제할 수 없습니다.
                    - deletedAt 필드만 세팅합니다.
                    """
    )
    ApiResponse<OrderResponse.Update> deleteOrder(
            @Parameter(description = "주문 ID", required = true)
            java.util.UUID orderId
    );
}