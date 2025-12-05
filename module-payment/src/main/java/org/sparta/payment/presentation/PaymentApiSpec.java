package org.sparta.payment.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.sparta.common.api.ApiResponse;
import org.sparta.payment.domain.enumeration.PaymentStatus;
import org.sparta.payment.presentation.dto.request.PaymentCancelRequest;
import org.sparta.payment.presentation.dto.request.PaymentCreateRequest;
import org.sparta.payment.presentation.dto.request.PaymentRefundPartialRequest;
import org.sparta.payment.presentation.dto.response.PaymentDetailResponse;
import org.sparta.payment.presentation.dto.response.PaymentListResponse;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 결제 API 명세
 */
@Tag(name = "Payment API", description = "결제 관리")
public interface PaymentApiSpec {

    @Operation(
            summary = "결제 생성",
            description = "새로운 결제를 생성합니다"
    )
    ApiResponse<PaymentDetailResponse> createPayment(
            @Valid @RequestBody PaymentCreateRequest request,
            @RequestHeader("X-User-Id") UUID userId
    );

    @Operation(
            summary = "결제 조회",
            description = "결제 ID로 결제 상세 정보를 조회합니다"
    )
    ApiResponse<PaymentDetailResponse> getPayment(
            @PathVariable UUID paymentId,
            @RequestHeader("X-User-Id") UUID userId
    );

    @Operation(
            summary = "주문 기준 결제 조회",
            description = "주문 ID로 결제 정보를 조회합니다"
    )
    ApiResponse<PaymentDetailResponse> getPaymentByOrderId(
            @PathVariable UUID orderId,
            @RequestHeader("X-User-Id") UUID userId
    );

    @Operation(
            summary = "상태 기준 결제 목록 조회",
            description = "결제 상태 기준으로 결제 목록을 조회합니다"
    )
    ApiResponse<PaymentListResponse> getPaymentsByStatus(
            @RequestParam(required = true) PaymentStatus status,
            @RequestHeader("X-User-Id") UUID userId
    );

    @Operation(
            summary = "결제 전체 취소",
            description = "해당 결제를 전체 취소 처리합니다"
    )
    ApiResponse<PaymentDetailResponse> cancelPayment(
            @PathVariable UUID paymentId,
            @Valid @RequestBody PaymentCancelRequest request,
            @RequestHeader("X-User-Id") UUID userId
    );

    @Operation(
            summary = "부분 환불",
            description = "해당 결제에 대해 부분 환불을 처리합니다"
    )
    ApiResponse<PaymentDetailResponse> refundPartial(
            @PathVariable UUID paymentId,
            @Valid @RequestBody PaymentRefundPartialRequest request,
            @RequestHeader("X-User-Id") UUID userId
    );

}
