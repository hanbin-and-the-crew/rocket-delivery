package org.sparta.payment.presentation.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.sparta.common.api.ApiResponse;
import org.sparta.payment.application.command.refund.RefundCreateCommand;
import org.sparta.payment.application.command.refund.RefundGetByIdCommand;
import org.sparta.payment.application.command.refund.RefundGetByPaymentIdCommand;
import org.sparta.payment.application.dto.RefundDetailResult;
import org.sparta.payment.application.dto.RefundListResult;
import org.sparta.payment.application.service.RefundService;
import org.sparta.payment.presentation.RefundApiSpec;
import org.sparta.payment.presentation.dto.request.RefundCreateRequest;
import org.sparta.payment.presentation.dto.response.RefundDetailResponse;
import org.sparta.payment.presentation.dto.response.RefundListResponse;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

@RestController
@RequestMapping("/refunds")
@RequiredArgsConstructor
public class RefundController implements RefundApiSpec {

    private final RefundService refundService;

    /**
     * 환불 생성 (직접 호출이 필요한 경우)
     * POST /refunds
     */
    @PostMapping
    @Override
    public ApiResponse<RefundDetailResponse> createRefund(@Valid @RequestBody RefundCreateRequest request,
                                                         @RequestHeader("X-User-Id") UUID userId) {
        RefundCreateCommand command = new RefundCreateCommand(
                request.paymentId(),
                request.amount(),
                request.reason()
        );
        RefundDetailResult result = refundService.createRefund(command);
        return ApiResponse.success(RefundDetailResponse.from(result));
    }

    /**
     * 환불 단건 조회
     * GET /refunds/{refundId}
     */
    @GetMapping("/{refundId}")
    @Override
    public ApiResponse<RefundDetailResponse> getRefund(@PathVariable UUID refundId,
                                                      @RequestHeader("X-User-Id") UUID userId) {
        RefundDetailResult result =
                refundService.getRefund(new RefundGetByIdCommand(refundId));
        return ApiResponse.success(RefundDetailResponse.from(result));
    }

    /**
     * 특정 결제에 대한 환불 이력 조회
     * GET /refunds/payment/{paymentId}
     */
    @GetMapping("/payment/{paymentId}")
    @Override
    public ApiResponse<RefundListResponse> getRefundsByPaymentId(@PathVariable UUID paymentId,
                                                                 @RequestHeader("X-User-Id") UUID userId) {
        RefundListResult result =
                refundService.getRefundsByPaymentId(new RefundGetByPaymentIdCommand(paymentId));
        return ApiResponse.success(RefundListResponse.from(result));
    }
}
