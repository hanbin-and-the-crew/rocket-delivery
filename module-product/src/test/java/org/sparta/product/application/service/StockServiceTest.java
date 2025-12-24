package org.sparta.product.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sparta.common.error.BusinessException;
import org.sparta.product.domain.error.ProductErrorType;
import org.sparta.product.domain.repository.StockRepository;
import org.sparta.product.domain.repository.StockReservationRepository;
import org.sparta.redis.util.DistributedLockExecutor;
import org.sparta.redis.util.LockAcquisitionException;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock
    private StockRepository stockRepository;

    @Mock
    private StockReservationRepository stockReservationRepository;

    @Mock
    private DistributedLockExecutor lockExecutor;

    @InjectMocks
    private StockService stockService;

    @Test
    @DisplayName("reserveStock_validation: productId/reservationKey/quantity 검증 실패 시 BusinessException(ProductErrorType) 발생")
    void reserveStock_validation() {
        UUID productId = UUID.randomUUID();

        BusinessException ex1 = assertThrows(BusinessException.class,
                () -> stockService.reserveStock(null, "rk", 1));
        assertEquals(ProductErrorType.PRODUCT_REQUIRED, ex1.getErrorType());

        BusinessException ex2 = assertThrows(BusinessException.class,
                () -> stockService.reserveStock(productId, null, 1));
        assertEquals(ProductErrorType.STOCK_RESERVATION_KEY_REQUIRED, ex2.getErrorType());

        BusinessException ex3 = assertThrows(BusinessException.class,
                () -> stockService.reserveStock(productId, "   ", 1));
        assertEquals(ProductErrorType.STOCK_RESERVATION_KEY_REQUIRED, ex3.getErrorType());

        BusinessException ex4 = assertThrows(BusinessException.class,
                () -> stockService.reserveStock(productId, "rk", 0));
        assertEquals(ProductErrorType.RESERVE_QUANTITY_INVALID, ex4.getErrorType());

        verifyNoInteractions(stockRepository, stockReservationRepository, lockExecutor);
    }

    @Test
    @DisplayName("reserveStock_lockBusy: 락 획득 실패(LockAcquisitionException)면 STOCK_LOCK_BUSY")
    void reserveStock_lockBusy() {
        UUID productId = UUID.randomUUID();

        when(lockExecutor.executeWithLock(
                startsWith("product:stock:lock:" + productId),
                anyLong(),
                anyLong(),
                eq(TimeUnit.SECONDS),
                any(Supplier.class)
        )).thenThrow(new LockAcquisitionException("busy"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> stockService.reserveStock(productId, "order-1", 1));

        assertEquals(ProductErrorType.STOCK_LOCK_BUSY, ex.getErrorType());
        verifyNoInteractions(stockRepository, stockReservationRepository);
    }
}
