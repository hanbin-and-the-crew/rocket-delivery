package org.sparta.order.infrastructure.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "payment-service"
)
public interface PaymentClient {
    @PostMapping("/api/payment/{orderId}/approve")
    PaymentApproveResponse approvePayment(
            @PathVariable("orderId") String orderId,
            @RequestBody PaymentApproveRequest request
    );

    // ===== DTO =====
    record PaymentApproveRequest(
            String orderId,
//            String pgToken,
            Long amountPayable,
            String methodType,   // "CARD"
            String pgProvider,   // "TOSS"
            String currency      // "KRW"
    ) {}

    record PaymentApproveResponse(
            String pgToken,
            String paymentKey    // 실제 결제 키 (가정)
    ) {}
}
