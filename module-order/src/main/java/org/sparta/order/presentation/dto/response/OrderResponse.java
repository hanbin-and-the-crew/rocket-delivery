package org.sparta.order.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.sparta.order.domain.entity.Order;
import org.sparta.order.domain.enumeration.CanceledReasonCode;
import org.sparta.order.domain.enumeration.OrderStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 주문 관련 Response DTO
 */
public class OrderResponse {

    @Schema(description = "주문 단건 상세 응답")
    public record Detail(

            @Schema(description = "주문 ID", example = "550e8400-e29b-41d4-a716-446655440000")
            UUID orderId,

            @Schema(description = "주문 상태", example = "CREATED")
            OrderStatus orderStatus,

            @Schema(description = "주문자(고객) ID", example = "550e8400-e29b-41d4-a716-446655440010")
            UUID customerId,

            @Schema(description = "공급업체 ID", example = "550e8400-e29b-41d4-a716-446655440001")
            UUID supplierCompanyId,

            @Schema(description = "공급업체 허브 ID", example = "550e8400-e29b-41d4-a716-446655440002")
            UUID supplierHubId,

            @Schema(description = "수령업체 ID", example = "550e8400-e29b-41d4-a716-446655440003")
            UUID receiptCompanyId,

            @Schema(description = "수령업체 허브 ID", example = "550e8400-e29b-41d4-a716-446655440004")
            UUID receiptHubId,

            @Schema(description = "상품 ID", example = "550e8400-e29b-41d4-a716-446655440005")
            UUID productId,

            @Schema(description = "주문 수량", example = "10")
            Integer quantity,

            @Schema(description = "상품 가격 스냅샷", example = "10000")
            Long productPriceSnapshot,

            @Schema(description = "총 주문 금액", example = "100000")
            Long totalPrice,

            @Schema(description = "배송지 주소", example = "서울특별시 강남구 테헤란로 123")
            String address,

            @Schema(description = "주문자 실명", example = "김손님")
            String userName,

            @Schema(description = "전화번호", example = "010-1111-2222")
            String userPhoneNumber,

            @Schema(description = "slack 아이디", example = "12@1234.com")
            String slackId,

            @Schema(description = "납품 기한", example = "2025-12-31T23:59:59")
            LocalDateTime dueAt,

            @Schema(description = "요청사항", example = "빠른 배송 부탁드립니다")
            String requestMemo,

            @Schema(description = "취소 사유 코드", example = "CUSTOMER_REQUEST")
            CanceledReasonCode canceledReasonCode,

            @Schema(description = "취소 사유 상세", example = "고객 요청으로 취소합니다")
            String canceledReasonMemo,

            @Schema(description = "취소 일시", example = "2025-12-01T10:00:00")
            LocalDateTime canceledAt,

            @Schema(description = "생성 일시", example = "2025-11-25T09:00:00")
            LocalDateTime createdAt,

            @Schema(description = "수정 일시", example = "2025-11-25T10:00:00")
            LocalDateTime updatedAt
    ) {

        public static Detail from(Order o) {
            return new Detail(
                    o.getId(),
                    o.getOrderStatus(),
                    o.getCustomerId(),
                    o.getSupplierCompanyId(),
                    o.getSupplierHubId(),
                    o.getReceiveCompanyId(),
                    o.getReceiveHubId(),
                    o.getProductId(),
                    o.getQuantity().getValue(),
                    o.getProductPriceSnapshot().getAmount(),
                    o.getTotalPrice().getAmount(),
                    o.getAddress(),
                    o.getUserName(),
                    o.getUserPhoneNumber(),
                    o.getSlackId(),
                    o.getDueAt().getTime(),
                    o.getRequestMemo(),
                    o.getCanceledReasonCode(),
                    o.getCanceledReasonMemo(),
                    o.getCanceledAt(),
                    o.getCreatedAt(),
                    o.getUpdatedAt()
            );
        }
    }

    @Schema(description = "주문 목록 조회용 요약 응답")
    public record Summary(

            @Schema(description = "주문 ID", example = "550e8400-e29b-41d4-a716-446655440000")
            UUID orderId,

            @Schema(description = "주문 상태", example = "CREATED")
            OrderStatus orderStatus,

            @Schema(description = "공급업체 ID", example = "550e8400-e29b-41d4-a716-446655440001")
            UUID supplierCompanyId,

            @Schema(description = "수령업체 ID", example = "550e8400-e29b-41d4-a716-446655440003")
            UUID receiptCompanyId,

            @Schema(description = "상품 ID", example = "550e8400-e29b-41d4-a716-446655440005")
            UUID productId,

            @Schema(description = "주문 수량", example = "10")
            Integer quantity,

            @Schema(description = "총 주문 금액", example = "100000")
            Long totalPrice,

            @Schema(description = "납품 기한", example = "2025-12-31T23:59:59")
            LocalDateTime dueAt,

            @Schema(description = "배송지 주소", example = "서울특별시 강남구 테헤란로 123")
            String address,

            @Schema(description = "생성 일시", example = "2025-11-25T09:00:00")
            LocalDateTime createdAt
    ) {

        public static Summary from(Order o) {
            return new Summary(
                    o.getId(),
                    o.getOrderStatus(),
                    o.getSupplierCompanyId(),
                    o.getReceiveCompanyId(),
                    o.getProductId(),
                    o.getQuantity().getValue(),
                    o.getTotalPrice().getAmount(),
                    o.getDueAt().getTime(),
                    o.getAddress(),
                    o.getCreatedAt()
            );
        }
    }

    @Schema(description = "주문 수정 응답")
    public record Update(
            @Schema(description = "주문 ID")
            UUID orderId,

            @Schema(description = "수정된 내용")
            String message,

            @Schema(description = "수정 시간")
            LocalDateTime updatedAt
    ) {
        public static Update of(Order order, String message) {
            return new Update(
                    order.getId(),
                    message,
                    order.getUpdatedAt()
            );
        }
    }
}
