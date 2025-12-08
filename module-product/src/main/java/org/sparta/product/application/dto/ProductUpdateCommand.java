package org.sparta.product.application.dto;

/**
 * Product 수정 시 사용하는 Application 계층 입력 모델
 * - null 인 필드는 수정하지 않음
 */
public record ProductUpdateCommand(
        String productName,
        Long price
) {
}
