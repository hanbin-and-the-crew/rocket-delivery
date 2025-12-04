package org.sparta.order.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.sparta.common.api.ApiResponse;
import org.sparta.order.presentation.dto.request.OrderRequest;
import org.sparta.order.presentation.dto.response.OrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Tag(name = "Order API", description = "주문 생성/조회/수정/취소/배송 처리 API")
public interface OrderApiSpec {

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

    @Operation(
            summary = "주문 단건 조회",
            description = "주문 ID로 주문 상세 정보를 조회합니다."
    )
    ApiResponse<OrderResponse.Detail> getOrder(
            @Parameter(description = "주문 ID", required = true)
            java.util.UUID orderId
    );

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

    @Operation(
            summary = "주문 납기일 변경",
            description = "CREATED 상태의 주문에 대해서만 납품 기한을 변경할 수 있습니다."
    )
    ApiResponse<OrderResponse.Update> changeDueAt(
            @Parameter(description = "주문 ID", required = true)
            java.util.UUID orderId,
            OrderRequest.ChangeDueAt request
    );

    @Operation(
            summary = "주문 주소 변경",
            description = "CREATED 상태의 주문에 대해서만 배송지 주소를 변경할 수 있습니다."
    )
    ApiResponse<OrderResponse.Update> changeAddress(
            @Parameter(description = "주문 ID", required = true)
            java.util.UUID orderId,
            OrderRequest.ChangeAddress request
    );

    @Operation(
            summary = "주문 요청사항 변경",
            description = "CREATED 상태의 주문에 대해서만 요청사항(메모)을 변경할 수 있습니다."
    )
    ApiResponse<OrderResponse.Update> changeRequestMemo(
            @Parameter(description = "주문 ID", required = true)
            java.util.UUID orderId,
            OrderRequest.ChangeMemo request
    );

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

    @Operation(
            summary = "주문 출고(배송 시작)",
            description = "APPROVED 상태의 주문을 SHIPPED 상태로 변경합니다."
    )
    ApiResponse<OrderResponse.Update> shipOrder(
            @Parameter(description = "주문 ID", required = true)
            java.util.UUID orderId
    );

    @Operation(
            summary = "주문 배송 완료 처리",
            description = "SHIPPED 상태의 주문을 DELIVERED 상태로 변경합니다."
    )
    ApiResponse<OrderResponse.Update> deliverOrder(
            @Parameter(description = "주문 ID", required = true)
            java.util.UUID orderId
    );

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
