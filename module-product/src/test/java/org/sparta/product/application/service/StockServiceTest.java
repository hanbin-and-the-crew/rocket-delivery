package org.sparta.product.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sparta.common.error.BusinessException;
import org.sparta.product.domain.entity.Stock;
import org.sparta.product.domain.error.ProductErrorType;
import org.sparta.product.domain.repository.StockRepository;
import org.sparta.product.support.fixtures.StockFixture;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;

/**
 * StockService 테스트
 *
 * 요구사항:
 * - 재고의 차감/복원은 Stock 애그리거트 내에서 이루어져야한다.
 * - Product 더티체킹을 방지하기 위해 Stock만 독립적으로 조회한다.
 *
 * 테스트 전략:
 * - Mock: StockRepository (외부 의존성 격리)
 * - 실제 객체: StockService (테스트 대상)
 * - Fixture: ProductFixture 사용으로 테스트 데이터 생성
 *
 * 핵심 검증 사항:
 * 1. 재고 차감 시 Stock만 조회하는지 (Product 조회 X)
 * 2. 재고 부족 시 예외가 발생하는지
 * 3. 재고 복원이 정상 동작하는지
 */
@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock
    private StockRepository stockRepository;

    @InjectMocks
    private StockService stockService;

    @Test
    @DisplayName("재고가 충분하면 차감이 성공한다")
    void decreaseStock_WithSufficientStock_ShouldSucceed() {
        // given: 재고 100개인 상품
        Stock stock = StockFixture.withQuantity(100);
        UUID productId = stock.getProductId();
        int decreaseQuantity = 30;

        given(stockRepository.findByProductId(productId))
                .willReturn(Optional.of(stock));

        // when: 재고 30개 차감
        stockService.decreaseStock(productId, decreaseQuantity);

        // then: 재고가 70개로 감소
        assertThat(stock.getQuantity()).isEqualTo(70);
        assertThat(stock.getAvailableQuantity()).isEqualTo(70);
    }

    @Test
    @DisplayName("재고가 부족하면 INSUFFICIENT_STOCK 예외가 발생한다")
    void decreaseStock_WithInsufficientStock_ShouldThrowException() {
        // given: 재고 5개인 상품
        Stock stock = StockFixture.withQuantity(5);
        UUID productId = stock.getProductId();
        int decreaseQuantity = 10;

        given(stockRepository.findByProductId(productId))
                .willReturn(Optional.of(stock));

        // when & then: 예외 발생
        assertThatThrownBy(() -> stockService.decreaseStock(productId, decreaseQuantity))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorType", ProductErrorType.INSUFFICIENT_STOCK);
    }

    @Test
    @DisplayName("유효한 수량으로 재고를 복원하면 성공한다")
    void increaseStock_WithValidQuantity_ShouldSucceed() {
        // given: 재고 50개인 상품
        Stock stock = StockFixture.withQuantity(50);
        UUID productId = stock.getProductId();
        int increaseQuantity = 20;

        given(stockRepository.findByProductId(productId))
                .willReturn(Optional.of(stock));

        // when: 재고 20개 복원
        stockService.increaseStock(productId, increaseQuantity);

        // then: 재고가 70개로 증가
        assertThat(stock.getQuantity()).isEqualTo(70);
    }

    @Test
    @DisplayName("존재하지 않는 재고를 차감하려 하면 STOCK_NOT_FOUND 예외가 발생한다")
    void decreaseStock_WithNonExistentStock_ShouldThrowException() {
        // given: 존재하지 않는 상품 ID
        UUID invalidProductId = UUID.randomUUID();

        given(stockRepository.findByProductId(invalidProductId))
                .willReturn(Optional.empty());

        // when & then: 예외 발생
        assertThatThrownBy(() -> stockService.decreaseStock(invalidProductId, 10))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorType", ProductErrorType.STOCK_NOT_FOUND);
    }

    @Test
    @DisplayName("존재하지 않는 재고를 복원하려 하면 STOCK_NOT_FOUND 예외가 발생한다")
    void increaseStock_WithNonExistentStock_ShouldThrowException() {
        // given: 존재하지 않는 상품 ID
        UUID invalidProductId = UUID.randomUUID();

        given(stockRepository.findByProductId(invalidProductId))
                .willReturn(Optional.empty());

        // when & then: 예외 발생
        assertThatThrownBy(() -> stockService.increaseStock(invalidProductId, 10))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorType", ProductErrorType.STOCK_NOT_FOUND);
    }

    @Test
    @DisplayName("가용 재고가 충분하면 예약에 성공한다")
    void reserveStock_WithSufficientStock_ShouldSucceed() {
        // given: 재고 100개인 상품
        Stock stock = StockFixture.withQuantity(100);
        UUID productId = stock.getProductId();
        int reserveQuantity = 30;

        given(stockRepository.findByProductId(productId))
                .willReturn(Optional.of(stock));

        // when: 재고 30개 예약
        stockService.reserveStock(productId, reserveQuantity);

        // then: 예약 재고가 30개로 증가
        assertThat(stock.getReservedQuantity()).isEqualTo(30);
        assertThat(stock.getQuantity()).isEqualTo(100);
        assertThat(stock.getAvailableQuantity()).isEqualTo(70);
    }

    @Test
    @DisplayName("가용 재고가 부족하면 예약에 실패한다")
    void reserveStock_WithInsufficientStock_ShouldThrowException() {
        // given: 재고 10개인 상품
        Stock stock = StockFixture.withQuantity(10);
        UUID productId = stock.getProductId();
        int reserveQuantity = 20;

        given(stockRepository.findByProductId(productId))
                .willReturn(Optional.of(stock));

        // when & then: 예외 발생
        assertThatThrownBy(() -> stockService.reserveStock(productId, reserveQuantity))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorType", ProductErrorType.INSUFFICIENT_STOCK);
    }

    @Test
    @DisplayName("예약 확정 시 실제 재고와 예약 재고가 모두 감소한다")
    void confirmReservation_ShouldDecreaseQuantityAndReservedQuantity() {
        // given: 재고 100개, 예약 30개인 상품
        Stock stock = StockFixture.withQuantity(100);
        UUID productId = stock.getProductId();
        stock.reserve(30);

        given(stockRepository.findByProductId(productId))
                .willReturn(Optional.of(stock));

        // when: 예약 30개 확정
        stockService.confirmReservation(productId, 30);

        // then: 실제 재고 70, 예약 재고 0
        assertThat(stock.getQuantity()).isEqualTo(70);
        assertThat(stock.getReservedQuantity()).isEqualTo(0);
        assertThat(stock.getAvailableQuantity()).isEqualTo(70);
    }

    @Test
    @DisplayName("예약 취소 시 예약 재고만 감소한다")
    void cancelReservation_ShouldOnlyDecreaseReservedQuantity() {
        // given: 재고 100개, 예약 30개인 상품
        Stock stock = StockFixture.withQuantity(100);
        UUID productId = stock.getProductId();
        stock.reserve(30);

        given(stockRepository.findByProductId(productId))
                .willReturn(Optional.of(stock));

        // when: 예약 10개 취소
        stockService.cancelReservation(productId, 10);

        // then: 실제 재고는 유지, 예약 재고만 감소
        assertThat(stock.getQuantity()).isEqualTo(100);
        assertThat(stock.getReservedQuantity()).isEqualTo(20);
        assertThat(stock.getAvailableQuantity()).isEqualTo(80);
    }
}
