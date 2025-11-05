package org.sparta.product.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.sparta.common.api.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController implements ProductApiSpec{

    @Override
    @PostMapping("/")
    public ApiResponse<ProductResponse.Create> createProduct(
            @Valid @RequestBody
            ProductRequest.Create request
    ) {
        // TODO: 실제 서비스 로직 구현 필요 테스트 용으로 임시로 생성
        ProductResponse.Create response = new ProductResponse.Create(
                1L,
                request.name(),
                request.description(),
                request.price(),
                request.stock()
        );
        return ApiResponse.success(response);
    }

}
