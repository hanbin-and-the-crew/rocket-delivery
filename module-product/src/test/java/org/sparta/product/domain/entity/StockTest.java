package org.sparta.product.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sparta.common.error.BusinessException;
import org.sparta.product.domain.enums.StockStatus;
import org.sparta.product.domain.error.ProductErrorType;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class StockTest {

    @Test
    @DisplayName("Stock.create: 필수값/재고량 검증 실패 시 BusinessException(ProductErrorType) 발생")
    void create_requiredValidation() {
        UUID productId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();

        BusinessException ex1 = assertThrows(BusinessException.class,
                () -> Stock.create(null, companyId, hubId, 0));
        assertEquals(ProductErrorType.PRODUCT_REQUIRED, ex1.getErrorType());

        BusinessException ex2 = assertThrows(BusinessException.class,
                () -> Stock.create(productId, null, hubId, 0));
        assertEquals(ProductErrorType.COMPANY_ID_REQUIRED, ex2.getErrorType());

        BusinessException ex3 = assertThrows(BusinessException.class,
                () -> Stock.create(productId, companyId, null, 0));
        assertEquals(ProductErrorType.HUB_ID_REQUIRED, ex3.getErrorType());

        BusinessException ex4 = assertThrows(BusinessException.class,
                () -> Stock.create(productId, companyId, hubId, -1));
        assertEquals(ProductErrorType.INITIAL_QUANTITY_INVALID, ex4.getErrorType());
    }

    @Test
    @DisplayName("Stock.reserve: 가용 재고 부족 시 INSUFFICIENT_STOCK")
    void reserve_insufficientStock() {
        Stock stock = Stock.create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 10);

        BusinessException ex = assertThrows(BusinessException.class, () -> stock.reserve(11));
        assertEquals(ProductErrorType.INSUFFICIENT_STOCK, ex.getErrorType());
    }

    @Test
    @DisplayName("Stock.reserve/confirm/cancel: reservedQuantity/quantity/status 전이")
    void reserve_confirm_cancel_statusTransitions() {
        Stock stock = Stock.create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 10);

        assertEquals(10, stock.getQuantity());
        assertEquals(0, stock.getReservedQuantity());
        assertEquals(StockStatus.IN_STOCK, stock.getStatus());

        stock.reserve(10);
        assertEquals(10, stock.getReservedQuantity());
        assertEquals(0, stock.getAvailableQuantity());
        assertEquals(StockStatus.RESERVED_ONLY, stock.getStatus());

        stock.cancelReservation(5);
        assertEquals(5, stock.getReservedQuantity());
        assertEquals(5, stock.getAvailableQuantity());
        assertEquals(StockStatus.IN_STOCK, stock.getStatus());

        stock.confirmReservation(5);
        assertEquals(5, stock.getQuantity());         // 10 - 5
        assertEquals(0, stock.getReservedQuantity()); // 5 - 5
        assertEquals(StockStatus.IN_STOCK, stock.getStatus());

        // OUT_OF_STOCK은 updateStatus()가 호출되는 흐름(예약/확정/취소)에서만 갱신된다.
        stock.reserve(5);
        assertEquals(5, stock.getReservedQuantity());
        assertEquals(0, stock.getAvailableQuantity());
        assertEquals(StockStatus.RESERVED_ONLY, stock.getStatus());

        stock.confirmReservation(5);
        assertEquals(0, stock.getQuantity());
        assertEquals(0, stock.getReservedQuantity());
        assertEquals(StockStatus.OUT_OF_STOCK, stock.getStatus());
    }

    @Test
    @DisplayName("Stock.confirmReservation: reservedQuantity 부족 시 INVALID_RESERVATION_CONFIRM")
    void confirm_invalidReservation() {
        Stock stock = Stock.create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 10);
        stock.reserve(3);

        BusinessException ex = assertThrows(BusinessException.class, () -> stock.confirmReservation(4));
        assertEquals(ProductErrorType.INVALID_RESERVATION_CONFIRM, ex.getErrorType());
    }

    @Test
    @DisplayName("Stock.cancelReservation: reservedQuantity 부족 시 INVALID_RESERVATION_CANCEL")
    void cancel_invalidReservation() {
        Stock stock = Stock.create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 10);
        stock.reserve(3);

        BusinessException ex = assertThrows(BusinessException.class, () -> stock.cancelReservation(4));
        assertEquals(ProductErrorType.INVALID_RESERVATION_CANCEL, ex.getErrorType());
    }

    @Test
    @DisplayName("UNAVAILABLE 상태에서는 reserve/decrease 시 STOCK_UNAVAILABLE")
    void unavailable_blocksReserveAndDecrease() {
        Stock stock = Stock.create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 10);
        stock.markAsUnavailable();
        assertEquals(StockStatus.UNAVAILABLE, stock.getStatus());

        BusinessException ex1 = assertThrows(BusinessException.class, () -> stock.reserve(1));
        assertEquals(ProductErrorType.STOCK_UNAVAILABLE, ex1.getErrorType());

        BusinessException ex2 = assertThrows(BusinessException.class, () -> stock.decrease(1));
        assertEquals(ProductErrorType.STOCK_UNAVAILABLE, ex2.getErrorType());
    }
}
