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
    public ProductResponse.Detail getProduct(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ProductErrorType.PRODUCT_NOT_FOUND));
        return ProductResponse.Detail.of(product);
    }

    /**
     * 상품 수정
     * - 상품명과 가격을 수정할 수 있음
     */
    @Transactional
    public ProductResponse.Update updateProduct(UUID productId, ProductRequest.Update request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ProductErrorType.PRODUCT_NOT_FOUND));

        Money price = request.price() != null ? Money.of(request.price()) : null;
        product.update(request.name(), price);

        Product updatedProduct = productRepository.save(product);
        return ProductResponse.Update.of(updatedProduct);
    }

    /**
     * 상품 삭제
     * - 논리적 삭제 (isActive = false)
     */
    @Transactional
    public void deleteProduct(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ProductErrorType.PRODUCT_NOT_FOUND));

        product.delete();
        productRepository.save(product);
    }

    private void validateCategoryExists(UUID categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new BusinessException(ProductErrorType.PRODUCT_NOT_FOUND);
        }
    }
}
