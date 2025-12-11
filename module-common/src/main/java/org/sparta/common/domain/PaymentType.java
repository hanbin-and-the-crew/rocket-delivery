package org.sparta.common.domain;

public enum PaymentType {
    CARD,              // 신용/체크카드
    VIRTUAL_ACCOUNT,   // 가상계좌
    ACCOUNT_TRANSFER,  // 계좌이체
    MOBILE,            // 휴대폰 결제
    NAVER_PAY,
    KAKAO_PAY
}
