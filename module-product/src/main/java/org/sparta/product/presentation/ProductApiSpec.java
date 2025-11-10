package org.sparta.product.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.sparta.common.api.ApiResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@Tag(name = "Product API",  description = "물류 상품 관리" )
public interface ProductApiSpec {

    @Operation(
            summary = "상품 생성",
            description = "새로운 상품을 생성합니다."
    )
    ApiResponse<ProductResponse.Create> createProduct(
            @Valid @RequestBody
            ProductRequest.Create request
    );

    @Operation(
            summary = "상품 조회",
            description = "상품 ID로 상품 정보를 조회합니다."
    )
    ApiResponse<ProductResponse.Detail> getProduct(
            @PathVariable UUID productId
    );

    @Operation(
            summary = "상품 수정",
            description = "상품 정보를 수정합니다."
    )
    ApiResponse<ProductResponse.Update> updateProduct(
            @PathVariable UUID productId,
            @Valid @RequestBody ProductRequest.Update request
    );

    @Operation(
            summary = "상품 삭제",
            description = "상품을 논리적으로 삭제합니다."
    )
    ApiResponse<Void> deleteProduct(
            @PathVariable UUID productId
    );

}
