package org.sparta.payment.presentation.controller;

import lombok.RequiredArgsConstructor;

import org.sparta.common.api.ApiResponse;

import org.sparta.payment.application.command.payment.*;
import org.sparta.payment.application.dto.PaymentDetailResult;
import org.sparta.payment.application.dto.PaymentListResult;
import org.sparta.payment.application.service.PaymentService;
import org.sparta.payment.domain.enumeration.PaymentStatus;
import org.sparta.payment.presentation.PaymentApiSpec;
import org.sparta.payment.presentation.dto.request.PaymentCancelRequest;
import org.sparta.payment.presentation.dto.request.PaymentCreateRequest;
import org.sparta.payment.presentation.dto.request.PaymentRefundPartialRequest;
import org.sparta.payment.presentation.dto.response.PaymentDetailResponse;
import org.sparta.payment.presentation.dto.response.PaymentListResponse;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController implements PaymentApiSpec {

    private final PaymentService paymentService;

    /**
     * 결제 생성
     * POST /payments
     */
    @PostMapping
    @Override
    public ApiResponse<PaymentDetailResponse> createPayment(@RequestBody PaymentCreateRequest request, @RequestHeader("X-User-Id") UUID userId) {
        PaymentCreateCommand command = new PaymentCreateCommand(
                request.orderId(),
                request.amountTotal(),
                request.amountCoupon(),
                request.amountPoint(),
                request.amountPayable(),
                request.methodType(),
                request.pgProvider(),
                request.currency(),
                request.couponId(),
                request.pointUsageId()
        );

        var result = paymentService.storeCompletedPayment(command, request.paymentKey());
        return ApiResponse.success(PaymentDetailResponse.from(result));
    }

    /**
     * 결제 단건 조회
     * GET /payments/{paymentId}
     */
    @Override
    @GetMapping("/{paymentId}")
    public ApiResponse<PaymentDetailResponse> getPayment(@PathVariable UUID paymentId, @RequestHeader("X-User-Id") UUID userId) {
        PaymentDetailResult result =
                paymentService.getPayment(new PaymentGetByIdCommand(paymentId));
        return ApiResponse.success(PaymentDetailResponse.from(result));
    }

    /**
     * 주문 기준 결제 조회
     * GET /payments/by-order/{orderId}
     */
    @GetMapping("/by-order/{orderId}")
    @Override
    public ApiResponse<PaymentDetailResponse> getPaymentByOrderId(@PathVariable UUID orderId, @RequestHeader("X-User-Id") UUID userId) {
        PaymentDetailResult result =
                paymentService.getPaymentByOrderId(new PaymentGetByOrderIdCommand(orderId));
        return ApiResponse.success(PaymentDetailResponse.from(result));
    }

    /**
     * 상태 기준 결제 목록 조회
     * GET /payments?status=COMPLETED
     */
    @GetMapping
    @Override
    public ApiResponse<PaymentListResponse> getPaymentsByStatus(@RequestParam(name = "status") PaymentStatus status, @RequestHeader("X-User-Id") UUID userId) {
        PaymentListResult result =
                paymentService.getPaymentsByStatus(new PaymentGetByStatusCommand(status));
        return ApiResponse.success(PaymentListResponse.from(result));
    }

    /**
     * 결제 전체 취소
     * POST /payments/{paymentId}/cancel
     */
    @PostMapping("/{paymentId}/cancel")
    @Override
    public ApiResponse<PaymentDetailResponse> cancelPayment(@PathVariable UUID paymentId,
                                               @RequestBody PaymentCancelRequest request,
                                               @RequestHeader("X-User-Id") UUID userId) {
        PaymentDetailResult result =
                paymentService.cancelPayment(
                        new PaymentCancelCommand(paymentId, request.reason())
                );
        return ApiResponse.success(PaymentDetailResponse.from(result));
    }

    /**
     * 부분 환불
     * POST /payments/{paymentId}/refund-partial
     */
    @PostMapping("/{paymentId}/refund-partial")
    @Override
    public ApiResponse<PaymentDetailResponse> refundPartial(@PathVariable UUID paymentId,
                                               @RequestBody PaymentRefundPartialRequest request,
                                               @RequestHeader("X-User-Id") UUID userId) {
        PaymentDetailResult result =
                paymentService.refundPartial(
                        new PaymentRefundPartialCommand(
                                paymentId,
                                request.refundAmount(),
                                request.reason()
                        )
                );
        return ApiResponse.success(PaymentDetailResponse.from(result));
    }

    /**
     * 결제 삭제
     * DELETE /payments/{paymentId}
     */
    @DeleteMapping("/{paymentId}")
    @Override
    public ApiResponse<Void> deletePayment(@PathVariable UUID paymentId, @RequestHeader("X-User-Id") UUID userId) {
        paymentService.deletePayment(new PaymentDeleteCommand(paymentId));
        return ApiResponse.success(null);
    }
}
