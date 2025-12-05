package org.sparta.payment.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.sparta.common.api.ApiResponse;
import org.sparta.payment.presentation.dto.request.RefundCreateRequest;
import org.sparta.payment.presentation.dto.response.RefundDetailResponse;
import org.sparta.payment.presentation.dto.response.RefundListResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

@Tag(name = "Refund API", description = "환불 관리")
public interface RefundApiSpec {

    @Operation(
            summary = "환불 생성",
            description = "특정 결제에 대해 환불을 생성합니다"
    )
    ApiResponse<RefundDetailResponse> createRefund(
            @Valid @RequestBody RefundCreateRequest request,
            @RequestHeader("X-User-Id") UUID userId
    );

    @Operation(
            summary = "환불 단건 조회",
            description = "refundId 로 환불 상세 정보를 조회합니다"
    )
    ApiResponse<RefundDetailResponse> getRefund(
            @PathVariable UUID refundId,
            @RequestHeader("X-User-Id") UUID userId
    );

    @Operation(
            summary = "결제 기준 환불 목록 조회",
            description = "paymentId 기준 환불 이력을 조회합니다"
    )
    ApiResponse<RefundListResponse> getRefundsByPaymentId(
            @PathVariable UUID paymentId,
            @RequestHeader("X-User-Id") UUID userId
    );
}
