package org.sparta.product.presentation.spec;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.sparta.common.api.ApiResponse;
import org.sparta.product.presentation.dto.stock.StockRequest;
import org.sparta.product.presentation.dto.stock.StockResponse;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Stock Reservation API", description = "상품 재고 예약/확정/취소 내부 API")
public interface StockApiSpec {

    @Operation(
            summary = "재고 예약",
            description = """
                    주문 생성 시 재고를 예약합니다.
                    
                    - 예약 키(reservationKey)는 주문 도메인이 생성하는 값입니다 (예: orderItemId 또는 "orderId:lineNo").
                    - 같은 reservationKey로 이미 예약된 경우:
                      - 예약 수량이 같으면 멱등하게 기존 예약을 그대로 반환합니다.
                      - 예약 수량이 다르면 에러를 반환합니다.
                    """
    )
    ApiResponse<StockResponse.Reserve> reserveStock(
            @Valid @RequestBody StockRequest.Reserve request
    );

    @Operation(
            summary = "재고 예약 확정",
            description = """
                    결제 성공 시 호출하여, 예약을 확정하고 실제 재고를 차감합니다.
                    
                    - 이미 확정된 예약에 대해 다시 호출하면 멱등하게 성공 응답을 반환합니다.
                    - 이미 취소된 예약을 확정하려 하면 에러를 반환합니다.
                    """
    )
    ApiResponse<Void> confirmReservation(
            @Valid @RequestBody StockRequest.Confirm request
    );

    @Operation(
            summary = "재고 예약 취소",
            description = """
                    주문 취소 또는 결제 실패 시 호출하여, 예약을 취소하고 예약 수량을 되돌립니다.
                    
                    - 이미 취소된 예약에 대해 다시 호출하면 멱등하게 성공 응답을 반환합니다.
                    - 이미 확정된 예약을 취소하려 하면 에러를 반환합니다.
                    """
    )
    ApiResponse<Void> cancelReservation(
            @Valid @RequestBody StockRequest.Cancel request
    );
}
