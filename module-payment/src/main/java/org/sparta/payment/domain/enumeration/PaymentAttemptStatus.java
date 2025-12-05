package org.sparta.payment.domain.enumeration;

public enum PaymentAttemptStatus {
    PENDING,      // PG 요청 보낸 시점
    SUCCESS,      // PG 승인 성공
    FAILED,       // 응답은 왔지만 실패
    TIMEOUT       // PG 응답 없음
}
