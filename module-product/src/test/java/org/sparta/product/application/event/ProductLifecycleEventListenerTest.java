package org.sparta.product.application.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sparta.product.domain.entity.Stock;
import org.sparta.product.domain.enums.StockStatus;
import org.sparta.product.domain.event.ProductCreatedEvent;
import org.sparta.product.domain.event.ProductDeletedEvent;
import org.sparta.product.domain.repository.StockRepository;
import org.sparta.product.support.fixtures.StockFixture;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * ProductLifecycleEventListener 테스트
 * - Product 생명주기 이벤트 처리 검증
 * - Stock 생성 및 상태 변경 로직 검증
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProductLifecycleEventListener 테스트")
class ProductLifecycleEventListenerTest {

    @Mock
    private StockRepository stockRepository;

    @InjectMocks
    private ProductLifecycleEventListener eventListener;

    @Test
    @DisplayName("Product 생성 이벤트를 받으면 Stock을 생성한다")
    void handleProductCreated_ShouldCreateStock() {
        // given: Product 생성 이벤트
        UUID productId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();
        Integer initialQuantity = 100;

        ProductCreatedEvent event = ProductCreatedEvent.of(
                productId,
                companyId,
                hubId,
                initialQuantity
        );

        given(stockRepository.save(any(Stock.class))).willAnswer(inv -> inv.getArgument(0));

        // when: 이벤트 처리
        eventListener.handleProductCreated(event);

        // then: Stock이 생성되어 저장됨
        verify(stockRepository).save(any(Stock.class));
    }

    @Test
    @DisplayName("Product 삭제 이벤트를 받으면 Stock을 판매 불가 상태로 변경한다")
    void handleProductDeleted_ShouldMarkStockAsUnavailable() {
        // given: Product 삭제 이벤트와 기존 Stock
        UUID productId = UUID.randomUUID();
        Stock stock = StockFixture.withProductId(productId);

        ProductDeletedEvent event = ProductDeletedEvent.of(productId);

        given(stockRepository.findByProductId(productId)).willReturn(Optional.of(stock));
        given(stockRepository.save(any(Stock.class))).willAnswer(inv -> inv.getArgument(0));

        // when: 이벤트 처리
        eventListener.handleProductDeleted(event);

        // then: Stock이 UNAVAILABLE 상태로 변경됨
        assertThat(stock.getStatus()).isEqualTo(StockStatus.UNAVAILABLE);
        verify(stockRepository).findByProductId(productId);
        verify(stockRepository).save(stock);
    }

    @Test
    @DisplayName("존재하지 않는 Product의 삭제 이벤트를 받으면 로그만 남기고 정상 처리된다")
    void handleProductDeleted_WithNonExistentProduct_ShouldLogAndContinue() {
        // given: 존재하지 않는 Product의 삭제 이벤트
        UUID nonExistentProductId = UUID.randomUUID();
        ProductDeletedEvent event = ProductDeletedEvent.of(nonExistentProductId);

        given(stockRepository.findByProductId(nonExistentProductId)).willReturn(Optional.empty());

        // when: 이벤트 처리
        eventListener.handleProductDeleted(event);

        // then: 예외를 던지지 않고 정상 처리됨
        verify(stockRepository).findByProductId(nonExistentProductId);
        verify(stockRepository, never()).save(any());
    }
}
