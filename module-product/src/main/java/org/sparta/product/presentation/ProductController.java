package org.sparta.product.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.sparta.common.api.ApiResponse;
import org.sparta.product.application.dto.ProductCreateCommand;
import org.sparta.product.application.dto.ProductDetailInfo;
import org.sparta.product.application.dto.ProductUpdateCommand;
import org.sparta.product.application.service.ProductService;
import org.sparta.product.presentation.dto.product.ProductRequest;
import org.sparta.product.presentation.dto.product.ProductResponse;
import org.sparta.product.presentation.spec.ProductApiSpec;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Product REST 컨트롤러
 *
 * - presentation → application 계층 매핑 담당
 * - ProductService 는 command/info 기반으로만 동작
 * - 외부로는 ProductRequest / ProductResponse 를 노출
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController implements ProductApiSpec {

    private final ProductService productService;

    // --------------------------------------------------------------------
    // 상품 생성
    // --------------------------------------------------------------------
    @Override
    @PostMapping
    public ApiResponse<ProductResponse.Create> createProduct(
            @Valid @RequestBody ProductRequest.Create request
    ) {
        // presentation → application command 매핑
        ProductCreateCommand command = new ProductCreateCommand(
                request.name(),
                request.price(),
                request.categoryId(),
                request.companyId(),
                request.hubId(),
                request.stock()
        );

        // 서비스 호출 (상품 + 재고 생성)
        UUID productId = productService.createProduct(command);

        // 생성 직후 상태를 다시 조회해서 응답 DTO로 변환
        ProductDetailInfo detail = productService.getProduct(productId);
        ProductResponse.Create response = ProductResponse.Create.of(detail);

        return ApiResponse.success(response);
    }

    // --------------------------------------------------------------------
    // 상품 단건 조회
    // --------------------------------------------------------------------
    @Override
    @GetMapping("/{productId}")
    public ApiResponse<ProductResponse.Detail> getProduct(
            @PathVariable UUID productId
    ) {
        ProductDetailInfo detail = productService.getProduct(productId);
        ProductResponse.Detail response = ProductResponse.Detail.of(detail);
        return ApiResponse.success(response);
    }

    // --------------------------------------------------------------------
    // 상품 수정
    // --------------------------------------------------------------------
    @Override
    @PatchMapping("/{productId}")
    public ApiResponse<ProductResponse.Update> updateProduct(
            @PathVariable UUID productId,
            @Valid @RequestBody ProductRequest.Update request
    ) {
        ProductUpdateCommand command = new ProductUpdateCommand(
                request.name(),
                request.price()
        );

        productService.updateProduct(productId, command);

        // 수정된 결과를 다시 조회해서 응답
        ProductDetailInfo detail = productService.getProduct(productId);
        ProductResponse.Update response = ProductResponse.Update.of(detail);

        return ApiResponse.success(response);
    }

    // --------------------------------------------------------------------
    // 상품 삭제
    // --------------------------------------------------------------------
    @Override
    @DeleteMapping("/{productId}")
    public ApiResponse<Void> deleteProduct(
            @PathVariable UUID productId
    ) {
        productService.deleteProduct(productId);
        return ApiResponse.success(null);
    }
}
