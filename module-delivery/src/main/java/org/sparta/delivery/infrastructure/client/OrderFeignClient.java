package org.sparta.delivery.infrastructure.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Order 모듈 Feign Client
 * - 주문 상태 조회 (정합성 체크용)
 */
@FeignClient(name = "order-service")
public interface OrderFeignClient {

    /**
     * 주문 단건 조회
     * @param orderId 주문 ID
     * @return ApiResponse로 감싸진 OrderResponse.Detail
     */
    @GetMapping("/api/orders/{orderId}")
    ApiResponse<OrderResponse.Detail> getOrder(@PathVariable("orderId") UUID orderId);

    // ===== Response DTO =====
    /**
     * Order 응답 DTO
     * - Delivery 모듈에서 필요한 필드만 정의
     */
    class OrderResponse {
        public record Detail(
                UUID orderId,
                OrderStatus orderStatus,           // 주문 상태 (CANCELLED 체크용)
                UUID customerId,
                UUID supplierCompanyId,
                UUID supplierHubId,
                UUID receiptCompanyId,
                UUID receiptHubId,
                UUID productId,
                Integer quantity,
                Long productPriceSnapshot,
                Long totalPrice,
                String address,
                String userName,
                String userPhoneNumber,
                String slackId,
                LocalDateTime dueAt,
                String requestMemo,
                CanceledReasonCode canceledReasonCode,  // 취소 사유 코드
                String canceledReasonMemo,              // 취소 사유 상세
                LocalDateTime canceledAt,               // 취소 일시
                LocalDateTime createdAt,
                LocalDateTime updatedAt
        ) {}
    }

    /**
     * 주문 상태 Enum
     * - Order 모듈의 OrderStatus와 동일하게 정의
     */
    enum OrderStatus {
        CREATED,            // 주문 생성
        APPROVED,           // 승인 (결제 완료)
        PREPARING_SHIPMENT, // 배송 준비 중
        SHIPPED,            // 배송 시작
        DELIVERED,          // 배송 완료
        CANCELLED           // 취소
    }

    /**
     * 취소 사유 코드 Enum
     * - Order 모듈의 CanceledReasonCode와 동일하게 정의
     */
    enum CanceledReasonCode {
        CUSTOMER_REQUEST,   // 고객 요청
        OUT_OF_STOCK,       // 재고 부족
        PAYMENT_FAILED,     // 결제 실패
        DELIVERY_FAILED,    // 배송 실패
        OTHER               // 기타
    }

    // ===== ApiResponse 래퍼 (meta로 감싸진 구조) =====
    /**
     * Order 모듈의 공통 API 응답 구조
     * - meta: 결과 메타 정보
     * - data: 실제 데이터
     */
    record ApiResponse<T>(
            Meta meta,
            T data
    ) {
        public boolean isSuccess() {
            return meta != null && "SUCCESS".equals(meta.result());
        }

        public String result() {
            return meta != null ? meta.result() : null;
        }

        public String errorCode() {
            return meta != null ? meta.errorCode() : null;
        }

        public String message() {
            return meta != null ? meta.message() : null;
        }
    }

    record Meta(
            String result,
            String errorCode,
            String message
    ) {}
}
