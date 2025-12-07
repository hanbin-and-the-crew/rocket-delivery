package org.sparta.product.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.sparta.common.error.BusinessException;
import org.sparta.product.domain.enums.StockStatus;
import org.sparta.product.support.fixtures.StockFixture;

import static org.assertj.core.api.Assertions.*;

/**
 * Stock 도메인 단위 테스트
 *
 * 도메인 요구사항:
 * 1. 재고 예약 시 가용 재고(quantity - reservedQuantity)를 확인한다
 * 2. 예약 확정 시 실제 재고와 예약 재고를 모두 차감한다
 * 3. 예약 취소 시 예약 재고만 차감한다
 * 4. 재고 상태는 자동으로 전환된다
 *    - IN_STOCK: 가용 재고 > 0
 *    - RESERVED_ONLY: 실물은 있지만 모두 예약됨
 *    - OUT_OF_STOCK: 실물 재고 없음
 *    - UNAVAILABLE: 판매 불가
 */
@DisplayName("Stock 재고 예약 비즈니스 로직 테스트")
class StockTest {

    @Test
    @DisplayName("가용 재고가 충분하면 예약에 성공한다")
    void reserve_WithSufficientStock_ShouldSucceed() {
        // given: 재고 100개
        Stock stock = StockFixture.withQuantity(100);
        int reserveQuantity = 30;

        // when: 30개 예약
        stock.reserve(reserveQuantity);

        // then
        assertThat(stock.getReservedQuantity()).isEqualTo(30);
        assertThat(stock.getQuantity()).isEqualTo(100);   // 실물 재고는 유지
        assertThat(stock.getAvailableQuantity()).isEqualTo(70);
        assertThat(stock.getStatus()).isEqualTo(StockStatus.IN_STOCK);
    }

    @Test
    @DisplayName("가용 재고가 부족하면 예약에 실패한다")
    void reserve_WithInsufficientStock_ShouldThrowException() {
        // given: 재고 10개
        Stock stock = StockFixture.withQuantity(10);
        int reserveQuantity = 20;

        // when & then
        assertThatThrownBy(() -> stock.reserve(reserveQuantity))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("재고가 부족합니다");
    }

    @Test
    @DisplayName("예약 후 가용 재고가 0이면 상태가 RESERVED_ONLY로 변경된다")
    void reserve_WhenAvailableBecomesZero_ShouldChangeStatusToReservedOnly() {
        // given
        Stock stock = StockFixture.withQuantity(100);

        // when: 100개 모두 예약
        stock.reserve(100);

        // then
        assertThat(stock.getQuantity()).isEqualTo(100);
        assertThat(stock.getReservedQuantity()).isEqualTo(100);
        assertThat(stock.getAvailableQuantity()).isEqualTo(0);
        assertThat(stock.getStatus()).isEqualTo(StockStatus.RESERVED_ONLY);
    }

    @Test
    @DisplayName("가용 재고가 있으면 IN_STOCK 상태가 된다")
    void getStatus_WhenAvailableStockPositive_ShouldBeInStock() {
        // given
        Stock stock = StockFixture.withQuantity(100);
        stock.reserve(30); // 예약 후에도 가용 재고 70

        // then
        assertThat(stock.getStatus()).isEqualTo(StockStatus.IN_STOCK);
        assertThat(stock.getAvailableQuantity()).isEqualTo(70);
    }

    @Test
    @DisplayName("가용 재고가 0이면 RESERVED_ONLY 상태가 된다")
    void getStatus_WhenReservedOnly_ShouldBeReservedOnly() {
        // given
        Stock stock = StockFixture.fullyReserved();

        // then
        assertThat(stock.getStatus()).isEqualTo(StockStatus.RESERVED_ONLY);
        assertThat(stock.getAvailableQuantity()).isEqualTo(0);
    }

    @Test
    @DisplayName("실물 재고가 0이면 OUT_OF_STOCK 상태가 된다")
    void getStatus_WhenOutOfStock_ShouldBeOutOfStock() {
        // given: 50개 재고, 50개 예약
        Stock stock = StockFixture.withQuantity(50);
        stock.reserve(50);

        // when: 50개 확정 → 실물 재고 0
        stock.confirmReservation(50);

        // then
        assertThat(stock.getQuantity()).isEqualTo(0);
        assertThat(stock.getReservedQuantity()).isEqualTo(0);
        assertThat(stock.getAvailableQuantity()).isEqualTo(0);
        assertThat(stock.getStatus()).isEqualTo(StockStatus.OUT_OF_STOCK);
    }

    @Test
    @DisplayName("예약 확정 시 실제 재고와 예약 재고가 모두 감소한다")
    void confirmReservation_ShouldDecreaseQuantityAndReservedQuantity() {
        // given: 재고 100, 예약 60
        Stock stock = StockFixture.withQuantity(100);
        stock.reserve(60);

        // when: 30개 확정
        stock.confirmReservation(30);

        // then: quantity 70, reserved 30, available 40
        assertThat(stock.getQuantity()).isEqualTo(70);
        assertThat(stock.getReservedQuantity()).isEqualTo(30);
        assertThat(stock.getAvailableQuantity()).isEqualTo(40);
    }

    @Test
    @DisplayName("예약되지 않은 수량을 확정하려 하면 예외가 발생한다")
    void confirmReservation_WithTooLargeQuantity_ShouldThrowException() {
        // given: 재고 100, 예약 20
        Stock stock = StockFixture.withQuantity(100);
        stock.reserve(20);

        // when & then: 30개 확정 시도
        assertThatThrownBy(() -> stock.confirmReservation(30))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("예약된 재고보다 많은 수량을 확정할 수 없습니다");
    }

    @Test
    @DisplayName("예약 취소 시 예약 재고만 감소하고 실제 재고는 유지된다")
    void cancelReservation_ShouldDecreaseReservedQuantityOnly() {
        // given: 재고 100, 예약 40
        Stock stock = StockFixture.withQuantity(100);
        stock.reserve(40);

        // when: 10개 취소
        stock.cancelReservation(10);

        // then: quantity 100, reserved 30, available 70
        assertThat(stock.getQuantity()).isEqualTo(100);
        assertThat(stock.getReservedQuantity()).isEqualTo(30);
        assertThat(stock.getAvailableQuantity()).isEqualTo(70);
    }

    @Test
    @DisplayName("예약되지 않은 수량을 취소하려 하면 예외가 발생한다")
    void cancelReservation_WithTooLargeQuantity_ShouldThrowException() {
        // given: 재고 100, 예약 20
        Stock stock = StockFixture.withQuantity(100);
        stock.reserve(20);

        // when & then: 30개 취소 시도
        assertThatThrownBy(() -> stock.cancelReservation(30))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("예약된 재고보다 많은 수량을 취소할 수 없습니다");
    }

    @Test
    @DisplayName("여러 번 예약해도 가용 재고 내에서만 가능하다")
    void reserve_MultipleTimesWithinAvailableStock_ShouldWorkCorrectly() {
        // given: 재고 100
        Stock stock = StockFixture.withQuantity(100);

        // when
        stock.reserve(30); // reserved 30, available 70
        stock.reserve(20); // reserved 50, available 50

        // then
        assertThat(stock.getReservedQuantity()).isEqualTo(50);
        assertThat(stock.getAvailableQuantity()).isEqualTo(50);

        // 가용 재고 초과 예약은 실패
        assertThatThrownBy(() -> stock.reserve(60))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("재고가 부족합니다");
    }

    @Test
    @DisplayName("예약 확정 후 실제 재고가 0이 되면 OUT_OF_STOCK 상태가 된다")
    void confirmReservation_ToZeroStock_ShouldBeOutOfStock() {
        // given: 재고 100, 예약 100
        Stock stock = StockFixture.withQuantity(100);
        stock.reserve(100);

        // when: 100개 확정
        stock.confirmReservation(100);

        // then
        assertThat(stock.getQuantity()).isEqualTo(0);
        assertThat(stock.getReservedQuantity()).isEqualTo(0);
        assertThat(stock.getStatus()).isEqualTo(StockStatus.OUT_OF_STOCK);
    }

    @Test
    @DisplayName("일부 예약 확정 후 나머지 취소도 정상 동작한다")
    void confirmAndCancelReservation_ShouldWorkCorrectly() {
        // given: 재고 100, 예약 60
        Stock stock = StockFixture.withQuantity(100);
        stock.reserve(60);

        // when: 30개 확정, 20개 취소 (총 60 중 50 처리)
        stock.confirmReservation(30);
        stock.cancelReservation(20);

        // then: quantity 70, reserved 10, available 60
        assertThat(stock.getQuantity()).isEqualTo(70);
        assertThat(stock.getReservedQuantity()).isEqualTo(10);
        assertThat(stock.getAvailableQuantity()).isEqualTo(60);
        assertThat(stock.getStatus()).isEqualTo(StockStatus.IN_STOCK);
    }

    @ParameterizedTest
    @CsvSource({
            "0",
            "-1",
            "-10"
    })
    @DisplayName("0 이하의 수량으로 예약하려 하면 예외가 발생한다")
    void reserve_WithInvalidQuantity_ShouldThrowException(int invalidQuantity) {
        // given: 재고 100개인 상품
        Stock stock = StockFixture.withQuantity(100);

        // when & then
        assertThatThrownBy(() -> stock.reserve(invalidQuantity))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("예약 수량은 1 이상이어야 합니다");
    }
}
