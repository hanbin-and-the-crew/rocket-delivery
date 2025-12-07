package org.sparta.product.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sparta.common.error.BusinessException;
import org.sparta.product.domain.entity.Stock;
import org.sparta.product.domain.entity.StockReservation;
import org.sparta.product.domain.enums.StockReservationStatus;
import org.sparta.product.domain.error.ProductErrorType;
import org.sparta.product.domain.repository.StockRepository;
import org.sparta.product.domain.repository.StockReservationRepository;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * StockService 애플리케이션 서비스 단위 테스트
 *
 * - 도메인 엔티티(Stock, StockReservation)의 세부 규칙은 도메인 테스트에서 검증
 * - 여기서는 "서비스가 어떤 순서와 정책으로 도메인/리포지토리를 호출하는지"를 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock
    private StockRepository stockRepository;

    @Mock
    private StockReservationRepository stockReservationRepository;

    @InjectMocks
    private StockService stockService;

    // 테스트에서 엔티티의 private id를 세팅하기 위한 유틸 메서드
    private static <T> void setField(T target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // === getStock ===========================================================

    @Test
    @DisplayName("존재하는 productId로 재고를 조회하면 Stock을 반환한다")
    void getStock_success() {
        // given
        UUID productId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();

        Stock stock = Stock.create(productId, companyId, hubId, 100);

        given(stockRepository.findByProductId(productId))
                .willReturn(Optional.of(stock));

        // when
        Stock result = stockService.getStock(productId);

        // then
        assertThat(result).isSameAs(stock);
    }

    @Test
    @DisplayName("존재하지 않는 productId로 재고를 조회하면 STOCK_NOT_FOUND 예외")
    void getStock_notFound_shouldThrowException() {
        // given
        UUID productId = UUID.randomUUID();
        given(stockRepository.findByProductId(productId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> stockService.getStock(productId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorType", ProductErrorType.STOCK_NOT_FOUND);
    }

    // === decreaseStock / increaseStock ======================================

    @Test
    @DisplayName("재고 차감 성공 시 quantity가 감소한다")
    void decreaseStock_success() {
        // given
        UUID productId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();

        Stock stock = Stock.create(productId, companyId, hubId, 100);

        given(stockRepository.findByProductId(productId))
                .willReturn(Optional.of(stock));

        // when
        stockService.decreaseStock(productId, 10);

        // then
        assertThat(stock.getQuantity()).isEqualTo(90);
        assertThat(stock.getReservedQuantity()).isZero(); // 단순 차감이라 예약 수량은 0 유지

        // decreaseStock에서는 save를 직접 호출하지 않고 변경 감지만 수행
        verify(stockRepository, never()).save(any(Stock.class));
        verifyNoInteractions(stockReservationRepository);
    }

    @Test
    @DisplayName("재고 차감 시 productId가 존재하지 않으면 STOCK_NOT_FOUND 예외 발생")
    void decreaseStock_stockNotFound_shouldThrowException() {
        // given
        UUID productId = UUID.randomUUID();
        given(stockRepository.findByProductId(productId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> stockService.decreaseStock(productId, 10))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorType", ProductErrorType.STOCK_NOT_FOUND);
    }

    @Test
    @DisplayName("재고 복원 성공 시 quantity가 증가한다")
    void increaseStock_success() {
        // given
        UUID productId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();

        Stock stock = Stock.create(productId, companyId, hubId, 100);

        given(stockRepository.findByProductId(productId))
                .willReturn(Optional.of(stock));

        // when
        stockService.increaseStock(productId, 30);

        // then
        assertThat(stock.getQuantity()).isEqualTo(130);
        assertThat(stock.getReservedQuantity()).isZero();

        verify(stockRepository, never()).save(any(Stock.class));
        verifyNoInteractions(stockReservationRepository);
    }

    @Test
    @DisplayName("재고 복원 시 productId가 존재하지 않으면 STOCK_NOT_FOUND 예외 발생")
    void increaseStock_stockNotFound_shouldThrowException() {
        // given
        UUID productId = UUID.randomUUID();
        given(stockRepository.findByProductId(productId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> stockService.increaseStock(productId, 10))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorType", ProductErrorType.STOCK_NOT_FOUND);
    }

    // === reserveStock =======================================================

    @Test
    @DisplayName("새로운 예약 - 재고가 충분하면 예약 엔티티를 생성하고 재고를 줄인다")
    void reserveStock_newReservation_success() {
        // given
        UUID productId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();

        String reservationKey = "order-1:item-1";
        int quantity = 30;

        Stock stock = Stock.create(productId, companyId, hubId, 100);
        setField(stock, "id", stockId);

        given(stockReservationRepository.findByReservationKey(reservationKey))
                .willReturn(Optional.empty());
        given(stockRepository.findByProductId(productId))
                .willReturn(Optional.of(stock));
        given(stockRepository.save(any(Stock.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(stockReservationRepository.save(any(StockReservation.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        StockReservation reservation = stockService.reserveStock(productId, reservationKey, quantity);

        // then
        assertThat(reservation.getStockId()).isEqualTo(stockId);
        assertThat(reservation.getReservationKey()).isEqualTo(reservationKey);
        assertThat(reservation.getReservedQuantity()).isEqualTo(quantity);
        assertThat(reservation.getStatus()).isEqualTo(StockReservationStatus.RESERVED);

        assertThat(stock.getReservedQuantity()).isEqualTo(quantity);
        assertThat(stock.getAvailableQuantity()).isEqualTo(100 - quantity);

        verify(stockReservationRepository).findByReservationKey(reservationKey);
        verify(stockRepository).findByProductId(productId);
        verify(stockReservationRepository).save(any(StockReservation.class));
        verify(stockRepository).save(any(Stock.class));
    }

    @Test
    @DisplayName("이미 동일한 예약 키와 수량으로 예약이 존재하면 멱등하게 기존 예약을 반환한다")
    void reserveStock_existingSameQuantity_idempotent() {
        // given
        UUID stockId = UUID.randomUUID();
        String reservationKey = "order-1:item-1";
        int quantity = 10;

        StockReservation existing = StockReservation.create(stockId, reservationKey, quantity);

        given(stockReservationRepository.findByReservationKey(reservationKey))
                .willReturn(Optional.of(existing));

        // when
        StockReservation result = stockService.reserveStock(UUID.randomUUID(), reservationKey, quantity);

        // then
        assertThat(result).isSameAs(existing);

        verify(stockReservationRepository).findByReservationKey(reservationKey);
        verifyNoMoreInteractions(stockReservationRepository);
        verifyNoInteractions(stockRepository);
    }

    @Test
    @DisplayName("이미 동일한 예약 키로 다른 수량이 예약되어 있으면 STOCK_RESERVATION_ALREADY_EXISTS 예외 발생")
    void reserveStock_existingDifferentQuantity_shouldThrowException() {
        // given
        UUID stockId = UUID.randomUUID();
        String reservationKey = "order-1:item-1";

        StockReservation existing = StockReservation.create(stockId, reservationKey, 10);

        given(stockReservationRepository.findByReservationKey(reservationKey))
                .willReturn(Optional.of(existing));

        // when & then
        assertThatThrownBy(() -> stockService.reserveStock(UUID.randomUUID(), reservationKey, 20))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorType", ProductErrorType.STOCK_RESERVATION_ALREADY_EXISTS);

        verify(stockReservationRepository).findByReservationKey(reservationKey);
        verifyNoMoreInteractions(stockReservationRepository);
        verifyNoInteractions(stockRepository);
    }

    // === confirmReservation ================================================

    @Test
    @DisplayName("예약 확정 - 정상 흐름에서는 재고 차감 + 예약 상태 CONFIRMED")
    void confirmReservation_success() {
        // given
        UUID productId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();

        String reservationKey = "order-1:item-1";
        int quantity = 10;

        Stock stock = Stock.create(productId, companyId, hubId, 100);
        setField(stock, "id", stockId);
        // 도메인 레벨에서 먼저 예약된 상태로 만들어 둔다
        stock.reserve(quantity);

        StockReservation reservation = StockReservation.create(stockId, reservationKey, quantity);

        given(stockReservationRepository.findByReservationKey(reservationKey))
                .willReturn(Optional.of(reservation));
        given(stockRepository.findById(stockId))
                .willReturn(Optional.of(stock));
        given(stockRepository.save(any(Stock.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(stockReservationRepository.save(any(StockReservation.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        stockService.confirmReservation(reservationKey);

        // then
        assertThat(stock.getQuantity()).isEqualTo(90);          // 실재 재고 차감
        assertThat(stock.getReservedQuantity()).isZero();       // 예약 수량 제거
        assertThat(reservation.getStatus()).isEqualTo(StockReservationStatus.CONFIRMED);

        verify(stockReservationRepository).findByReservationKey(reservationKey);
        verify(stockRepository).findById(stockId);
        verify(stockRepository).save(stock);
        verify(stockReservationRepository).save(reservation);
    }

    @Test
    @DisplayName("예약 확정 - 이미 CONFIRMED 상태면 멱등 처리 (아무 일도 일어나지 않는다)")
    void confirmReservation_alreadyConfirmed_idempotent() {
        // given
        UUID stockId = UUID.randomUUID();
        String reservationKey = "order-1:item-1";

        StockReservation reservation = StockReservation.create(stockId, reservationKey, 10);
        reservation.confirm(); // 이미 확정 상태

        given(stockReservationRepository.findByReservationKey(reservationKey))
                .willReturn(Optional.of(reservation));

        // when
        stockService.confirmReservation(reservationKey);

        // then
        assertThat(reservation.getStatus()).isEqualTo(StockReservationStatus.CONFIRMED);

        // 멱등 처리로 인해 Stock 은 조회조차 되지 않는다
        verify(stockReservationRepository).findByReservationKey(reservationKey);
        verifyNoMoreInteractions(stockReservationRepository);
        verifyNoInteractions(stockRepository);
    }

    @Test
    @DisplayName("예약 확정 - 이미 CANCELLED 상태면 STOCK_RESERVATION_ALREADY_CANCELLED 예외")
    void confirmReservation_alreadyCancelled_shouldThrowException() {
        // given
        UUID stockId = UUID.randomUUID();
        String reservationKey = "order-1:item-1";

        StockReservation reservation = StockReservation.create(stockId, reservationKey, 10);
        reservation.cancel(); // 이미 취소 상태

        given(stockReservationRepository.findByReservationKey(reservationKey))
                .willReturn(Optional.of(reservation));

        // when & then
        assertThatThrownBy(() -> stockService.confirmReservation(reservationKey))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorType", ProductErrorType.STOCK_RESERVATION_ALREADY_CANCELLED);

        verify(stockReservationRepository).findByReservationKey(reservationKey);
        verifyNoMoreInteractions(stockReservationRepository);
        verifyNoInteractions(stockRepository);
    }

    @Test
    @DisplayName("예약 확정 - reservationKey에 해당하는 예약이 없으면 STOCK_RESERVATION_NOT_FOUND 예외")
    void confirmReservation_reservationNotFound_shouldThrowException() {
        // given
        String reservationKey = "no-such-key";
        given(stockReservationRepository.findByReservationKey(reservationKey))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> stockService.confirmReservation(reservationKey))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorType", ProductErrorType.STOCK_RESERVATION_NOT_FOUND);
    }

    // === cancelReservation ==================================================

    @Test
    @DisplayName("예약 취소 - 정상 흐름에서는 예약 수량만 복원되고 상태는 CANCELLED")
    void cancelReservation_success() {
        // given
        UUID productId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();

        String reservationKey = "order-1:item-1";
        int quantity = 10;

        Stock stock = Stock.create(productId, companyId, hubId, 100);
        setField(stock, "id", stockId);
        // 먼저 예약된 상태로 만든다
        stock.reserve(quantity);

        StockReservation reservation = StockReservation.create(stockId, reservationKey, quantity);

        given(stockReservationRepository.findByReservationKey(reservationKey))
                .willReturn(Optional.of(reservation));
        given(stockRepository.findById(stockId))
                .willReturn(Optional.of(stock));
        given(stockRepository.save(any(Stock.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(stockReservationRepository.save(any(StockReservation.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        stockService.cancelReservation(reservationKey);

        // then
        assertThat(stock.getQuantity()).isEqualTo(100);         // 실재 재고는 그대로
        assertThat(stock.getReservedQuantity()).isZero();       // 예약 수량만 복원
        assertThat(reservation.getStatus()).isEqualTo(StockReservationStatus.CANCELLED);

        verify(stockReservationRepository).findByReservationKey(reservationKey);
        verify(stockRepository).findById(stockId);
        verify(stockRepository).save(stock);
        verify(stockReservationRepository).save(reservation);
    }

    @Test
    @DisplayName("예약 취소 - 이미 CANCELLED 상태면 멱등 처리 (아무 일도 일어나지 않는다)")
    void cancelReservation_alreadyCancelled_idempotent() {
        // given
        UUID stockId = UUID.randomUUID();
        String reservationKey = "order-1:item-1";

        StockReservation reservation = StockReservation.create(stockId, reservationKey, 10);
        reservation.cancel(); // 이미 취소 상태

        given(stockReservationRepository.findByReservationKey(reservationKey))
                .willReturn(Optional.of(reservation));

        // when
        stockService.cancelReservation(reservationKey);

        // then
        assertThat(reservation.getStatus()).isEqualTo(StockReservationStatus.CANCELLED);

        verify(stockReservationRepository).findByReservationKey(reservationKey);
        verifyNoMoreInteractions(stockReservationRepository);
        verifyNoInteractions(stockRepository);
    }

    @Test
    @DisplayName("예약 취소 - 이미 CONFIRMED 상태면 STOCK_RESERVATION_ALREADY_CONFIRMED 예외")
    void cancelReservation_alreadyConfirmed_shouldThrowException() {
        // given
        UUID stockId = UUID.randomUUID();
        String reservationKey = "order-1:item-1";

        StockReservation reservation = StockReservation.create(stockId, reservationKey, 10);
        reservation.confirm(); // 이미 확정 상태

        given(stockReservationRepository.findByReservationKey(reservationKey))
                .willReturn(Optional.of(reservation));

        // when & then
        assertThatThrownBy(() -> stockService.cancelReservation(reservationKey))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorType", ProductErrorType.STOCK_RESERVATION_ALREADY_CONFIRMED);

        verify(stockReservationRepository).findByReservationKey(reservationKey);
        verifyNoMoreInteractions(stockReservationRepository);
        verifyNoInteractions(stockRepository);
    }

    @Test
    @DisplayName("예약 취소 - reservationKey에 해당하는 예약이 없으면 STOCK_RESERVATION_NOT_FOUND 예외")
    void cancelReservation_reservationNotFound_shouldThrowException() {
        // given
        String reservationKey = "no-such-key";
        given(stockReservationRepository.findByReservationKey(reservationKey))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> stockService.cancelReservation(reservationKey))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorType", ProductErrorType.STOCK_RESERVATION_NOT_FOUND);
    }
}
