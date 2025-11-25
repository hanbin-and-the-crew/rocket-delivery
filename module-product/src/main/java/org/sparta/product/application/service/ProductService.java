package org.sparta.product.application.service;

import lombok.RequiredArgsConstructor;
import org.sparta.common.error.BusinessException;
import org.sparta.common.event.EventPublisher;
import org.sparta.product.domain.entity.Product;
import org.sparta.product.domain.error.ProductErrorType;
import org.sparta.product.domain.event.ProductCreatedEvent;
import org.sparta.product.domain.event.ProductDeletedEvent;
import org.sparta.product.domain.entity.Stock;
import org.sparta.product.domain.repository.CategoryRepository;
import org.sparta.product.domain.repository.ProductRepository;
import org.sparta.product.domain.repository.StockRepository;
import org.sparta.product.domain.vo.Money;
import org.sparta.product.presentation.ProductRequest;
import org.sparta.product.presentation.ProductResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Product 서비스
 * - 상품 생성 및 조회 담당
 * - Stock 생성은 이벤트를 통해 처리 (독립된 생명주기)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final StockRepository stockRepository;
    private final EventPublisher eventPublisher;

    /**
     * 상품 생성
     * - Product 저장 후 이벤트 발행
     * - Stock은 이벤트 리스너에서 생성됨 (독립된 생명주기)
     */
    @Transactional
    public ProductResponse.Create createProduct(ProductRequest.Create request) {

        validateCategoryExists(request.categoryId());
        Money price = Money.of(request.price());

        // Product 생성
        Product product = Product.create(
                request.name(),
                price,
                request.categoryId(),
                request.companyId(),
                request.hubId(),
                request.stock()
        );

        Product createdProduct = productRepository.save(product);

        // Product 생성 이벤트 발행 (Stock 생성 트리거)
        ProductCreatedEvent event = ProductCreatedEvent.of(
                createdProduct.getId(),
                createdProduct.getCompanyId(),
                createdProduct.getHubId(),
                request.stock()
        );
        eventPublisher.publishLocal(event);

        return ProductResponse.Create.of(createdProduct);
    }

    /**
     * 상품 조회
     * - Product와 Stock을 별도로 조회
     */
    public ProductResponse.Detail getProduct(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ProductErrorType.PRODUCT_NOT_FOUND));

        Stock stock = stockRepository.findByProductId(productId)
                .orElseThrow(() -> new BusinessException(ProductErrorType.STOCK_NOT_FOUND));

        return ProductResponse.Detail.of(product, stock);
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
     * - 삭제 이벤트 발행으로 Stock도 판매 불가 처리됨
     */
    @Transactional
    public void deleteProduct(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ProductErrorType.PRODUCT_NOT_FOUND));

        product.delete();
        productRepository.save(product);

        // Product 삭제 이벤트 발행 (Stock 판매 불가 처리 트리거)
        ProductDeletedEvent event = ProductDeletedEvent.of(productId);
        eventPublisher.publishLocal(event);
    }

    private void validateCategoryExists(UUID categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new BusinessException(ProductErrorType.PRODUCT_NOT_FOUND);
        }
    }
}
