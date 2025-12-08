package org.sparta.product.application.dto;

import java.util.UUID;

/**
 * Product 생성 시 사용하는 Application 계층 입력 모델
 */
public record ProductCreateCommand(
        String productName,
        long price,
        UUID categoryId,
        UUID companyId,
        UUID hubId,
        int initialQuantity
) {
}
