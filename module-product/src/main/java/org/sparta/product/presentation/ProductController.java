package org.sparta.product.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.sparta.common.api.ApiResponse;
import org.sparta.product.application.service.ProductService;
import org.sparta.product.presentation.dto.product.ProductRequest;
import org.sparta.product.presentation.dto.product.ProductResponse;
import org.sparta.product.presentation.spec.ProductApiSpec;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController implements ProductApiSpec {

    private final ProductService productService;

    @Override
    @PostMapping
    public ApiResponse<ProductResponse.Create> createProduct(
            @Valid @RequestBody
            ProductRequest.Create request
    ) {
        ProductResponse.Create response = productService.createProduct(request);
        return ApiResponse.success(response);
    }

    @Override
    @GetMapping("/{productId}")
    public ApiResponse<ProductResponse.Detail> getProduct(
            @PathVariable UUID productId
    ) {
        ProductResponse.Detail response = productService.getProduct(productId);
        return ApiResponse.success(response);
    }

    @Override
    @PatchMapping("/{productId}")
    public ApiResponse<ProductResponse.Update> updateProduct(
            @PathVariable UUID productId,
            @Valid @RequestBody ProductRequest.Update request
    ) {
        ProductResponse.Update response = productService.updateProduct(productId, request);
        return ApiResponse.success(response);
    }

    @Override
    @DeleteMapping("/{productId}")
    public ApiResponse<Void> deleteProduct(
            @PathVariable UUID productId
    ) {
        productService.deleteProduct(productId);
        return ApiResponse.success(null);
    }

}
