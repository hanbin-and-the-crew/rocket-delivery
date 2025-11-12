package org.sparta.order.application.dto.response;

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

    @Schema(description = "주문 생성 응답")
    public record Create(
            @Schema(description = "주문 ID")
            UUID orderId,

            @Schema(description = "주문 상태")
            OrderStatus status,

            @Schema(description = "총 금액")
            Long totalPrice,

            @Schema(description = "생성 시간")
            LocalDateTime createdAt
    ) {
        public static Create of(Order order) {
            return new Create(
                    order.getId(),
                    order.getOrderStatus(),
                    order.getTotalPrice().getAmount(),
                    order.getCreatedAt()
            );
        }
    }

    @Schema(description = "주문 상세 조회 응답")
    public record Detail(
            @Schema(description = "주문 ID")
            UUID orderId,

            @Schema(description = "배송 ID")
            UUID deliveryId,

            @Schema(description = "주문 상태")
            OrderStatus status,

            @Schema(description = "요청자 ID")
            UUID supplierId,

            @Schema(description = "요청업체 ID")
            UUID supplierCompanyId,

            @Schema(description = "요청업체 허브 ID")
            UUID supplierHubId,

            @Schema(description = "수령업체 ID")
            UUID receiptCompanyId,

            @Schema(description = "수령업체 허브 ID")
            UUID receiptHubId,

            @Schema(description = "상품 ID")
            UUID productId,

            @Schema(description = "상품명 (스냅샷)")
            String productName,

            @Schema(description = "상품 단가 (스냅샷)")
            Long productPrice,

            @Schema(description = "주문 수량")
            Integer quantity,

            @Schema(description = "총 금액")
            Long totalPrice,

            @Schema(description = "배송지 주소")
            String deliveryAddress,            
            
            @Schema(description = "주문자 실명")
            String userName,            
            
            @Schema(description = "전화번호")
            String userPhoneNumber,            
            
            @Schema(description = "slack 아이디")
            String slackId,

            @Schema(description = "납품 기한")
            LocalDateTime dueAt,

            @Schema(description = "요청사항")
            String requestedMemo,

            @Schema(description = "출고 시간")
            LocalDateTime dispatchedAt,

            @Schema(description = "출고 처리자 ID")
            UUID dispatchedBy,

            @Schema(description = "취소 시간")
            LocalDateTime canceledAt,

            @Schema(description = "취소자 ID")
            UUID canceledBy,

            @Schema(description = "취소 사유 코드")
            CanceledReasonCode canceledReasonCode,

            @Schema(description = "취소 사유 상세")
            String canceledReasonMemo,

            @Schema(description = "생성 시간")
            LocalDateTime createdAt,

            @Schema(description = "수정 시간")
            LocalDateTime updatedAt
    ) {
        public static Detail of(Order order) {
            return new Detail(
                    order.getId(),
                    order.getDeliveryId(),
                    order.getOrderStatus(),
                    order.getSupplierId(),
                    order.getSupplierCompanyId(),
                    order.getSupplierHubId(),
                    order.getReceiptCompanyId(),
                    order.getReceiptHubId(),
                    order.getProductId(),
                    order.getProductNameSnapshot(),
                    order.getProductPriceSnapshot().getAmount(),
                    order.getQuantity().getValue(),
                    order.getTotalPrice().getAmount(),
                    order.getAddressSnapshot(),
                    order.getUserName(),
                    order.getUserPhoneNumber(),
                    order.getSlackId(),
                    order.getDueAt(),
                    order.getRequestedMemo(),
                    order.getDispatchedAt(),
                    order.getDispatchedBy(),
                    order.getCanceledAt(),
                    order.getCanceledBy(),
                    order.getCanceledReasonCode(),
                    order.getCanceledReasonMemo(),
                    order.getCreatedAt(),
                    order.getUpdatedAt()
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

    @Schema(description = "주문 목록 조회 응답")
    public record Summary(
            @Schema(description = "주문 ID")
            UUID orderId,

            @Schema(description = "주문자 실명")
            String userName,

            @Schema(description = "주문 상태")
            OrderStatus status,

            @Schema(description = "상품명")
            String productName,

            @Schema(description = "주문 수량")
            Integer quantity,

            @Schema(description = "총 금액")
            Long totalPrice,

            @Schema(description = "납품 기한")
            LocalDateTime dueAt,

            @Schema(description = "생성 시간")
            LocalDateTime createdAt,

            @Schema(description = "수정 시간")
            LocalDateTime updatedAt
    ) {
        public static Summary of(Order order) {
            return new Summary(
                    order.getId(),
                    order.getUserName(),
                    order.getOrderStatus(),
                    order.getProductNameSnapshot(),
                    order.getQuantity().getValue(),
                    order.getTotalPrice().getAmount(),
                    order.getDueAt(),
                    order.getCreatedAt(),
                    order.getUpdatedAt()
            );
        }
    }
}