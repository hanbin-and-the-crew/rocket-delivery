package org.sparta.product.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.sparta.common.api.ApiResponse;
import org.springframework.web.bind.annotation.RequestBody;

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

}
