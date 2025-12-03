package org.sparta.product.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.sparta.common.api.ApiResponse;
import org.sparta.product.application.service.StockService;
import org.sparta.product.domain.entity.StockReservation;
import org.sparta.product.presentation.dto.stock.StockRequest;
import org.sparta.product.presentation.dto.stock.StockResponse;
import org.sparta.product.presentation.spec.StockApiSpec;
import org.springframework.web.bind.annotation.*;

/**
 * 재고 예약/확정/취소 REST 컨트롤러
 *
 * Gateway 기준 외부 경로:
 *  - /product/stocks/reserve  -> /api/product/stocks/reserve
 *  - /product/stocks/confirm  -> /api/product/stocks/confirm
 *  - /product/stocks/cancel   -> /api/product/stocks/cancel
 */
@RestController
@RequestMapping("/api/product/stocks")
@RequiredArgsConstructor
public class StockController implements StockApiSpec {

    private final StockService stockService;

    @Override
    @PostMapping("/reserve")
    public ApiResponse<StockResponse.Reserve> reserveStock(
            @Valid @RequestBody StockRequest.Reserve request
    ) {
        // 서비스 호출
        StockReservation reservation = stockService.reserveStock(
                request.productId(),
                request.reservationKey(),
                request.quantity()
        );

        // 응답 DTO 변환
        StockResponse.Reserve response = StockResponse.Reserve.of(reservation);
        return ApiResponse.success(response);
    }

    @Override
    @PostMapping("/confirm")
    public ApiResponse<Void> confirmReservation(
            @Valid @RequestBody StockRequest.Confirm request
    ) {
        stockService.confirmReservation(request.reservationKey());
        return ApiResponse.success(null);
    }

    @Override
    @PostMapping("/cancel")
    public ApiResponse<Void> cancelReservation(
            @Valid @RequestBody StockRequest.Cancel request
    ) {
        stockService.cancelReservation(request.reservationKey());
        return ApiResponse.success(null);
    }
}
