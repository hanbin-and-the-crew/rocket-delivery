package org.sparta.payment.presentation.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.sparta.common.api.ApiResponse;

import org.sparta.payment.application.command.payment.PaymentApprovalCommand;
import org.sparta.payment.application.dto.PaymentApprovalResult;
import org.sparta.payment.application.service.PaymentApprovalService;
import org.sparta.payment.presentation.dto.request.PaymentApprovalRequest;
import org.sparta.payment.presentation.dto.response.PaymentApprovalResponse;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/internal/payments")
@RequiredArgsConstructor
public class PaymentApprovalController {

    private final PaymentApprovalService paymentApprovalService;

    @PostMapping("/approve")
    public ApiResponse<PaymentApprovalResponse> approve(
            @Valid @RequestBody PaymentApprovalRequest request,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        PaymentApprovalCommand command = new PaymentApprovalCommand(
                request.orderId(),
                request.pgToken(),
                request.amountPayable(),
                request.methodType(),
                request.pgProvider(),
                request.currency()
        );

        PaymentApprovalResult result = paymentApprovalService.approve(command, userId);

        return ApiResponse.success(PaymentApprovalResponse.from(result));
    }
}
