package org.sparta.product.application.service;

import lombok.RequiredArgsConstructor;
import org.sparta.common.error.BusinessException;
import org.sparta.product.domain.entity.Product;
import org.sparta.product.domain.error.ProductErrorType;
import org.sparta.product.domain.repository.CategoryRepository;
import org.sparta.product.domain.repository.ProductRepository;
import org.sparta.product.domain.vo.Money;
import org.sparta.product.presentation.ProductRequest;
import org.sparta.product.presentation.ProductResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Product 서비스
 * - 상품 생성 및 조회 담당
 * - Stock 생성은 Product.create() 내부에서 처리 (생명주기 관리)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    /**
     * 상품 생성
     * - Product.create() 내부에서 Stock도 함께 생성됨 (Cascade)
     * - 고민점, : 추후 msa 에서는 어떻게 검증 id를 받아서 처리할까?
     */
    @Transactional
    public ProductResponse.Create createProduct(ProductRequest.Create request) {

        validateCategoryExists(request.categoryId());
        Money price = Money.of(request.price());
        // Product 생성
        // (우선은 companyId,hubId검증 없이 DTO 값 그대로 사용)
        Product product = Product.create(
                request.name(),
                price,
                request.categoryId(),
                request.companyId(),
                request.hubId(),
                request.stock()
        );

        Product createProduct = productRepository.save(product);
        return ProductResponse.Create.of(createProduct);
    }

    /**
     * 상품 조회
     */
    public Product getProduct(UUID productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ProductErrorType.PRODUCT_NOT_FOUND));
    }

    private void validateCategoryExists(UUID categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new BusinessException(ProductErrorType.PRODUCT_NOT_FOUND);
        }
    }
}
