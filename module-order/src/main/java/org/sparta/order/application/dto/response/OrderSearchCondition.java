package org.sparta.order.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.sparta.order.domain.enumeration.OrderStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 주문 검색 조건
 */
@Schema(description = "주문 검색 조건")
public record OrderSearchCondition(
        @Schema(description = "공급자 ID")
        UUID supplierId,

        @Schema(description = "수령업체 ID")
        UUID receiptCompanyId,

        @Schema(description = "상품 ID")
        UUID productId,

        @Schema(description = "주문 상태")
        OrderStatus status,

        @Schema(description = "검색 시작일")
        LocalDateTime startDate,

        @Schema(description = "검색 종료일")
        LocalDateTime endDate,

        @Schema(description = "납품 기한 시작일")
        LocalDateTime dueStartDate,

        @Schema(description = "납품 기한 종료일")
        LocalDateTime dueEndDate
) {
    public static OrderSearchCondition of(
            UUID supplierId,
            UUID receiptCompanyId,
            UUID productId,
            OrderStatus status,
            LocalDateTime startDate,
            LocalDateTime endDate,
            LocalDateTime dueStartDate,
            LocalDateTime dueEndDate
    ) {
        return new OrderSearchCondition(
                supplierId,
                receiptCompanyId,
                productId,
                status,
                startDate,
                endDate,
                dueStartDate,
                dueEndDate
        );
    }
}