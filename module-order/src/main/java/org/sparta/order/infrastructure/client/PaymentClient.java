package org.sparta.order.infrastructure.client;

import org.sparta.common.domain.PaymentType;
import org.sparta.common.domain.PgProvider;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.time.LocalDateTime;
import java.util.UUID;

@FeignClient(name = "payment-service")
public interface PaymentClient {

    @PostMapping("/api/payments/approve")
    ApiResponse<PaymentResponse.Approval> approve(
            @RequestBody PaymentRequest.Approval request,
            @RequestHeader("X-User-Id") UUID userId  // 필수 헤더
    );

    // ===== Request DTO =====
    class PaymentRequest {
        public record Approval(
                UUID orderId,
                String pgToken,
                Long amountPayable,
                PaymentType methodType,
                PgProvider pgProvider,
                String currency
        ) {}
    }

    // ===== Response DTO =====
    class PaymentResponse {
        public record Approval(
                UUID orderId,
                boolean approved,
                String paymentKey,
                LocalDateTime approvedAt,
                String failureCode,
                String failureMessage
        ) {}
    }

    // ===== ApiResponse 래퍼 =====
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
