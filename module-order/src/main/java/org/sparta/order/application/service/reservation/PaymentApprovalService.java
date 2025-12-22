package org.sparta.order.application.service.reservation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.common.domain.PaymentType;
import org.sparta.common.domain.PgProvider;
import org.sparta.common.error.BusinessException;
import org.sparta.order.application.dto.PaymentApprovalResult;
import org.sparta.order.application.error.ServiceUnavailableException;
import org.sparta.order.domain.circuitbreaker.CircuitBreaker;
import org.sparta.order.domain.circuitbreaker.CircuitBreakerOpenException;
import org.sparta.order.domain.error.OrderErrorType;
import org.sparta.order.infrastructure.client.PaymentClient;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * 결제 승인 서비스
 *
 * 단일 책임: 결제 승인 로직 + Circuit Breaker + 예외 변환
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentApprovalService {

    private static final String SERVICE_NAME = "payment-service";

    private final PaymentClient paymentClient;
    private final CircuitBreaker circuitBreaker;
    private final ObjectMapper objectMapper;

    /**
     * 결제 승인
     *
     * @param orderId 주문 ID
     * @param customerId 고객 ID
     * @param amountPayable 결제 금액
     * @param methodType 결제 수단 타입
     * @param pgProviderStr PG사
     * @param currency 통화
     * @return 결제 승인 결과
     * @throws BusinessException 결제 승인 실패 시
     */
    public PaymentApprovalResult approve(
        UUID orderId,
        UUID customerId,
        Long amountPayable,
        String methodType,
        String pgProviderStr,
        String currency
    ) {
        log.info("[결제] 승인 시작 - orderId={}, amountPayable={}, customerId={}",
            orderId, amountPayable, customerId);

        // customerId null 체크
        if (customerId == null) {
            log.error("[결제] customerId가 null입니다!");
            throw new BusinessException(OrderErrorType.CUSTOMER_ID_REQUIRED);
        }

        // PaymentType 변환
        PaymentType paymentType;
        try {
            paymentType = PaymentType.valueOf(methodType.toUpperCase());
            log.info("[결제] PaymentType 변환 성공: {} -> {}", methodType, paymentType);
        } catch (IllegalArgumentException e) {
            log.error("[결제] PaymentType 변환 실패: {}", methodType, e);
            throw new BusinessException(OrderErrorType.INVALID_PAYMENT_TYPE);
        }

        // PgProvider 변환
        PgProvider pgProvider;
        try {
            pgProvider = PgProvider.valueOf(pgProviderStr.toUpperCase());
            log.info("[결제] PgProvider 변환 성공: {} -> {}", pgProviderStr, pgProvider);
        } catch (IllegalArgumentException e) {
            log.error("[결제] PgProvider 변환 실패: {}", pgProviderStr, e);
            throw new BusinessException(OrderErrorType.INVALID_Pg_Provider);
        }

        try {
            String pgToken = UUID.randomUUID().toString();

            PaymentClient.PaymentRequest.Approval paymentRequest =
                new PaymentClient.PaymentRequest.Approval(
                    orderId, pgToken, amountPayable, paymentType, pgProvider, currency
                );

            // 전송될 JSON 로그
            try {
                String requestJson = objectMapper.writeValueAsString(paymentRequest);
                log.info("[결제] 전송 JSON: {}", requestJson);
            } catch (JsonProcessingException e) {
                log.error("[결제] JSON 직렬화 실패", e);
            }

            log.info("[결제] PaymentClient 호출 시작 - customerId={}", customerId);

            // Circuit Breaker로 감싼 결제 승인 호출
            PaymentClient.ApiResponse<PaymentClient.PaymentResponse.Approval> apiResponse =
                circuitBreaker.execute(
                    () -> paymentClient.approve(paymentRequest, customerId),
                    SERVICE_NAME
                );

            log.info("[결제] API 응답 수신 - result={}, errorCode={}, message={}",
                apiResponse.result(), apiResponse.errorCode(), apiResponse.message());

            // API 호출 결과 확인
            if (!apiResponse.isSuccess()) {
                log.error("[결제] API 호출 실패 - errorCode={}, message={}",
                    apiResponse.errorCode(), apiResponse.message());
                throw new BusinessException(OrderErrorType.PAYMENT_APPROVE_FAILED);
            }

            PaymentClient.PaymentResponse.Approval paymentData = apiResponse.data();

            if (paymentData == null) {
                log.error("[결제] 응답 데이터가 null");
                throw new BusinessException(OrderErrorType.PAYMENT_APPROVE_FAILED);
            }

            log.info("[결제] 데이터 추출 - orderId={}, approved={}, paymentKey={}, approvedAt={}",
                paymentData.orderId(), paymentData.approved(),
                paymentData.paymentKey(), paymentData.approvedAt());

            // 결제 승인 거부 확인
            if (!paymentData.approved()) {
                log.error("[결제] 결제 승인 거부 - failureCode={}, failureMessage={}",
                    paymentData.failureCode(), paymentData.failureMessage());
                throw new BusinessException(OrderErrorType.PAYMENT_APPROVE_FAILED);
            }

            log.info("[결제] 승인 완료 - orderId={}, paymentKey={}", orderId, paymentData.paymentKey());

            return new PaymentApprovalResult(
                paymentData.orderId(),
                paymentData.paymentKey(),
                paymentData.approved(),
                paymentData.approvedAt() != null ? paymentData.approvedAt().toString() : null
            );

        } catch (CircuitBreakerOpenException e) {
            log.warn("[결제] Circuit Breaker OPEN - service unavailable");
            throw new ServiceUnavailableException("결제 서비스가 일시적으로 중단되었습니다. 잠시 후 다시 시도해 주세요.");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[결제] Feign 호출 실패", e);
            throw new BusinessException(OrderErrorType.PAYMENT_APPROVE_FAILED);
        }
    }
}
