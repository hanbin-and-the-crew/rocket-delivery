//package org.sparta.product.domain.entity;
//
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.params.ParameterizedTest;
//import org.junit.jupiter.params.provider.CsvSource;
//import org.sparta.common.error.BusinessException;
//import org.sparta.product.domain.enums.StockStatus;
//import org.sparta.product.support.fixtures.StockFixture;
//
//import static org.assertj.core.api.Assertions.*;
//
///**
// * Stock 도메인 단위 테스트
// *
// * Step 5: 복잡한 비즈니스 로직 - 재고 예약 시스템
// *
// * 도메인 요구사항:
// * 1. 재고 예약 시 가용 재고(quantity - reservedQuantity)를 확인한다
// * 2. 예약 확정 시 실제 재고와 예약 재고를 모두 차감한다
// * 3. 예약 취소 시 예약 재고만 차감한다
// * 4. 재고 상태는 자동으로 전환된다
// *    - IN_STOCK: 가용 재고 > 0
// *    - RESERVED_ONLY: 실물은 있지만 모두 예약됨
// *    - OUT_OF_STOCK: 실물 재고 없음
// */
//@DisplayName("Stock 재고 예약 비즈니스 로직 테스트")
//class StockTest {
//
//    @Test
//    @DisplayName("가용 재고가 충분하면 예약에 성공한다")
//    void reserve_WithSufficientStock_ShouldSucceed() {
//        // given: 재고 100개
//        Stock stock = StockFixture.withQuantity(100);
//        int reserveQuantity = 30;
//
//        // when: 30개 예약
//        stock.reserve(reserveQuantity);
//
//        // then: 예약 재고가 30개로 증가
//        assertThat(stock.getReservedQuantity()).isEqualTo(30);
//        assertThat(stock.getQuantity()).isEqualTo(100); // 실제 재고는 유지
//        assertThat(stock.getAvailableQuantity()).isEqualTo(70); // 가용 재고 70
//    }
//
//    @Test
//    @DisplayName("가용 재고가 부족하면 예약에 실패한다")
//    void reserve_WithInsufficientStock_ShouldThrowException() {
//        // given: 재고 10개인 상품
//        Stock stock = StockFixture.withQuantity(10);
//        int reserveQuantity = 20;
//
//        // when & then: 20개 예약 시도 시 예외 발생
//        assertThatThrownBy(() -> stock.reserve(reserveQuantity))
//                .isInstanceOf(BusinessException.class)
//                .hasMessageContaining("재고가 부족합니다");
//    }
//
//    @Test
//    @DisplayName("예약 후 가용 재고가 0이면 상태가 RESERVED_ONLY로 변경된다")
//    void reserve_WhenAvailableBecomesZero_ShouldChangeStatusToReservedOnly() {
//        // given: 재고 100개인 상품
//        Stock stock = StockFixture.withQuantity(100);
//
//        // when: 100개 모두 예약
//        stock.reserve(100);
//
//        // then: 상태가 RESERVED_ONLY로 변경
//        assertThat(stock.getStatus()).isEqualTo(StockStatus.RESERVED_ONLY);
//        assertThat(stock.getQuantity()).isEqualTo(100);
//        assertThat(stock.getReservedQuantity()).isEqualTo(100);
//        assertThat(stock.getAvailableQuantity()).isEqualTo(0);
//    }
//
//    @Test
//    @DisplayName("가용 재고가 있으면 IN_STOCK 상태가 된다")
//    void updateStatus_WithAvailableStock_ShouldBeInStock() {
//        // given: 재고 100개, 예약 50개
//        Stock stock = StockFixture.withQuantity(100);
//
//        // when: 50개 예약
//        stock.reserve(50);
//
//        // then: 가용 재고 50개이므로 IN_STOCK
//        assertThat(stock.getStatus()).isEqualTo(StockStatus.IN_STOCK);
//        assertThat(stock.getAvailableQuantity()).isEqualTo(50);
//    }
//
//    @Test
//    @DisplayName("가용 재고가 0이면 RESERVED_ONLY 상태가 된다")
//    void updateStatus_WithZeroAvailable_ShouldBeReservedOnly() {
//        // given: 재고 100개
//        Stock stock = StockFixture.withQuantity(100);
//
//        // when: 100개 모두 예약
//        stock.reserve(100);
//
//        // then: 실물은 100이지만 모두 예약됨
//        assertThat(stock.getStatus()).isEqualTo(StockStatus.RESERVED_ONLY);
//        assertThat(stock.getQuantity()).isEqualTo(100);
//        assertThat(stock.getReservedQuantity()).isEqualTo(100);
//        assertThat(stock.getAvailableQuantity()).isEqualTo(0);
//    }
//
//    @Test
//    @DisplayName("실물 재고가 0이면 OUT_OF_STOCK 상태가 된다")
//    void updateStatus_WithZeroQuantity_ShouldBeOutOfStock() {
//        // given: 재고 50개, 50개 예약
//        Stock stock = StockFixture.withQuantity(50);
//        stock.reserve(50);
//
//        // when: 50개 확정 (실물 재고 0으로)
//        stock.confirmReservation(50);
//
//        // then: 실물 재고 0이므로 OUT_OF_STOCK
//        assertThat(stock.getStatus()).isEqualTo(StockStatus.OUT_OF_STOCK);
//        assertThat(stock.getQuantity()).isEqualTo(0);
//        assertThat(stock.getReservedQuantity()).isEqualTo(0);
//    }
//
//    @Test
//    @DisplayName("예약 확정 시 실제 재고와 예약 재고가 모두 감소한다")
//    void confirmReservation_ShouldDecreaseQuantityAndReservedQuantity() {
//        // given: 재고 100개, 예약 30개인 상품
//        Stock stock = StockFixture.withQuantity(100);
//        stock.reserve(30);
//
//        // when: 예약 30개 확정
//        stock.confirmReservation(30);
//
//        // then: 실제 재고 70, 예약 재고 0
//        assertThat(stock.getQuantity()).isEqualTo(70);
//        assertThat(stock.getReservedQuantity()).isEqualTo(0);
//        assertThat(stock.getAvailableQuantity()).isEqualTo(70);
//        assertThat(stock.getStatus()).isEqualTo(StockStatus.IN_STOCK);
//    }
//
//    @Test
//    @DisplayName("예약되지 않은 수량을 확정하려 하면 예외가 발생한다")
//    void confirmReservation_WithInvalidAmount_ShouldThrowException() {
//        // given: 재고 100개, 예약 10개인 상품
//        Stock stock = StockFixture.withQuantity(100);
//        stock.reserve(10);
//
//        // when & then: 20개 확정 시도 시 예외 발생
//        assertThatThrownBy(() -> stock.confirmReservation(20))
//                .isInstanceOf(BusinessException.class)
//                .hasMessageContaining("예약된 재고보다 많은 수량을 확정할 수 없습니다");
//    }
//
//    @Test
//    @DisplayName("예약 취소 시 예약 재고만 감소한다")
//    void cancelReservation_ShouldOnlyDecreaseReservedQuantity() {
//        // given: 재고 100개, 예약 30개인 상품
//        Stock stock = StockFixture.withQuantity(100);
//        stock.reserve(30);
//
//        // when: 예약 10개 취소
//        stock.cancelReservation(10);
//
//        // then: 실제 재고는 유지, 예약 재고만 감소
//        assertThat(stock.getQuantity()).isEqualTo(100);
//        assertThat(stock.getReservedQuantity()).isEqualTo(20);
//        assertThat(stock.getAvailableQuantity()).isEqualTo(80);
//        assertThat(stock.getStatus()).isEqualTo(StockStatus.IN_STOCK);
//    }
//
//    @Test
//    @DisplayName("예약되지 않은 수량을 취소하려 하면 예외가 발생한다")
//    void cancelReservation_WithInvalidAmount_ShouldThrowException() {
//        // given: 재고 100개, 예약 10개인 상품
//        Stock stock = StockFixture.withQuantity(100);
//        stock.reserve(10);
//
//        // when & then: 20개 취소 시도 시 예외 발생
//        assertThatThrownBy(() -> stock.cancelReservation(20))
//                .isInstanceOf(BusinessException.class)
//                .hasMessageContaining("예약된 재고보다 많은 수량을 취소할 수 없습니다");
//    }
//
//    @Test
//    @DisplayName("여러 번 예약해도 가용 재고 내에서만 가능하다")
//    void reserve_MultipleReservations_ShouldWorkWithinAvailableStock() {
//        // given: 재고 100개인 상품
//        Stock stock = StockFixture.withQuantity(100);
//
//        // when: 30개, 40개, 20개 순차 예약
//        stock.reserve(30);
//        stock.reserve(40);
//        stock.reserve(20);
//
//        // then: 총 90개 예약됨
//        assertThat(stock.getReservedQuantity()).isEqualTo(90);
//        assertThat(stock.getAvailableQuantity()).isEqualTo(10);
//
//        // when: 추가 20개 예약 시도
//        // then: 예외 발생 (가용 재고 10개만 남음)
//        assertThatThrownBy(() -> stock.reserve(20))
//                .isInstanceOf(BusinessException.class);
//    }
//
//    @Test
//    @DisplayName("예약 확정 후 실제 재고가 0이 되면 OUT_OF_STOCK 상태가 된다")
//    void confirmReservation_WhenQuantityBecomesZero_ShouldChangeToOutOfStock() {
//        // given: 재고 50개, 50개 예약된 상품
//        Stock stock = StockFixture.withQuantity(50);
//        stock.reserve(50);
//
//        // when: 50개 모두 확정
//        stock.confirmReservation(50);
//
//        // then: 상태가 OUT_OF_STOCK으로 변경
//        assertThat(stock.getQuantity()).isEqualTo(0);
//        assertThat(stock.getReservedQuantity()).isEqualTo(0);
//        assertThat(stock.getStatus()).isEqualTo(StockStatus.OUT_OF_STOCK);
//    }
//
//    @Test
//    @DisplayName("일부 예약 확정 후 나머지 취소도 정상 동작한다")
//    void partialConfirmAndCancel_ShouldWorkCorrectly() {
//        // given: 재고 100개, 60개 예약된 상품
//        Stock stock = StockFixture.withQuantity(100);
//        stock.reserve(60);
//
//        // when: 30개 확정, 20개 취소
//        stock.confirmReservation(30);
//        stock.cancelReservation(20);
//
//        // then: 실제 재고 70, 예약 재고 10
//        assertThat(stock.getQuantity()).isEqualTo(70);
//        assertThat(stock.getReservedQuantity()).isEqualTo(10);
//        assertThat(stock.getAvailableQuantity()).isEqualTo(60);
//        assertThat(stock.getStatus()).isEqualTo(StockStatus.IN_STOCK);
//    }
//
//    @ParameterizedTest
//    @CsvSource({
//            "0",
//            "-1",
//            "-10"
//    })
//    @DisplayName("0 이하의 수량으로 예약하려 하면 예외가 발생한다")
//    void reserve_WithInvalidQuantity_ShouldThrowException(int invalidQuantity) {
//        // given: 재고 100개인 상품
//        Stock stock = StockFixture.withQuantity(100);
//
//        // when & then: 잘못된 수량으로 예약 시도
//        assertThatThrownBy(() -> stock.reserve(invalidQuantity))
//                .isInstanceOf(BusinessException.class)
//                .hasMessageContaining("예약 수량은 1 이상이어야 합니다");
//    }
//}