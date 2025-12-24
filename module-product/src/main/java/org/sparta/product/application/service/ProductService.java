package org.sparta.product.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.common.error.BusinessException;
import org.sparta.product.application.dto.ProductCreateCommand;
import org.sparta.product.application.dto.ProductDetailInfo;
import org.sparta.product.application.dto.ProductUpdateCommand;
import org.sparta.product.domain.entity.Product;
import org.sparta.product.domain.entity.Stock;
import org.sparta.product.domain.error.ProductErrorType;
import org.sparta.product.domain.repository.CategoryRepository;
import org.sparta.product.domain.repository.ProductRepository;
import org.sparta.product.domain.repository.StockRepository;
import org.sparta.product.domain.vo.Money;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Product 서비스 (REST/동기 기반)
 * - 상품 메타데이터(Product) CRUD
 * - 상품 생성/삭제 시 재고(Stock)를 동기적으로 함께 처리
 *
 * 기존 구조:
 *  - ProductCreatedEvent / ProductDeletedEvent 를 이벤트로 발행하고
 *    Stock 쪽에서 리스닝하던 구조였음.
 *
 * 현재 구조:
 *  - 같은 모듈 안에서 Product 와 Stock 을 함께 다루므로
 *    동기 방식으로 바로 Stock 을 생성/판매불가 처리한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final StockRepository stockRepository;

    /**
     * 상품 생성
     *
     */
    @Transactional
    public UUID createProduct(ProductCreateCommand command) {

        validateCategoryExists(command.categoryId());

        Money price = Money.of(command.price());

        // Product 도메인 생성 (이름은 엔티티 기준: productName)
        Product product = Product.create(
                command.productName(),
                price,
                command.categoryId(),
                command.companyId(),
                command.hubId(),
                command.initialQuantity()
        );

        Product savedProduct = productRepository.save(product);

        // Stock 동기 생성 (이벤트 기반 → 직접 생성으로 전환)
        Stock stock = Stock.create(
                savedProduct.getId(),
                command.companyId(),
                command.hubId(),
                command.initialQuantity()
        );

        stockRepository.save(stock);

        log.info("[ProductService] 상품 생성 완료 productId={}, initialQuantity={}",
                savedProduct.getId(), command.initialQuantity());

        return savedProduct.getId();
    }

    /**
     * 상품 상세 조회
     * - Product, Stock 을 각각 조회해 통합 결과 반환
     */
    public ProductDetailInfo getProduct(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ProductErrorType.PRODUCT_NOT_FOUND));

        Stock stock = stockRepository.findByProductId(productId)
                .orElseThrow(() -> new BusinessException(ProductErrorType.STOCK_NOT_FOUND));

        return ProductDetailInfo.of(product, stock);
    }

    /**
     * 상품 수정
     * - 상품명과 가격을 선택적으로 수정 (null 이면 기존 값 유지)
     */
    @Transactional
    public void updateProduct(UUID productId, ProductUpdateCommand command) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ProductErrorType.PRODUCT_NOT_FOUND));

        Money price = command.price() != null ? Money.of(command.price()) : null;

        // 도메인 엔티티가 스스로 유효성 체크 및 변경
        product.update(command.productName(), price);

        // JPA 더티체킹으로도 가능하지만, 명시적으로 save 한 번 태워준다
        productRepository.save(product);

        log.info("[ProductService] 상품 수정 완료 productId={}", productId);
    }

    /**
     * 상품 삭제
     *
     * 흐름:
     *  1) Product 조회
     *  2) product.delete() 로 논리 삭제 (isActive=false)
     *  3) Stock 조회 후 markAsUnavailable() 로 판매 불가 상태 변경
     */
    @Transactional
    public void deleteProduct(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ProductErrorType.PRODUCT_NOT_FOUND));

        // 상품 논리 삭제
        product.delete();
        productRepository.save(product);

        // 재고 판매 불가 상태로 전환 (이전에는 ProductDeletedEvent 리스너에서 처리)
        stockRepository.findByProductId(productId)
                .ifPresent(stock -> {
                    stock.markAsUnavailable();
                    stockRepository.save(stock);
                });

        log.info("[ProductService] 상품 삭제 처리 완료 productId={}", productId);
    }

    /**
     * 카테고리 존재 여부 검증
     * - 존재하지 않으면 비즈니스 예외 발생
     */
    private void validateCategoryExists(UUID categoryId) {
        // null 체크는 Product 엔티티 내부 validateCategoryId(...) 에서도 한 번 더 진행됨
        if (categoryId == null || !categoryRepository.existsById(categoryId)) {
            throw new BusinessException(ProductErrorType.CATEGORY_ID_REQUIRED);
        }
    }
}
