package org.sparta.product.integration;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sparta.common.error.BusinessException;
import org.sparta.product.application.service.StockService;
import org.sparta.product.domain.entity.Category;
import org.sparta.product.domain.entity.Product;
import org.sparta.product.domain.entity.Stock;
import org.sparta.product.domain.entity.StockReservation;
import org.sparta.product.domain.repository.CategoryRepository;
import org.sparta.product.domain.repository.ProductRepository;
import org.sparta.product.domain.repository.StockRepository;
import org.sparta.product.domain.vo.Money;
import org.sparta.product.infrastructure.jpa.StockJpaRepository;
import org.sparta.product.infrastructure.jpa.StockReservationJpaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * StockService + JPA + DB 까지 실제로 돌려보는 동시성 통합 테스트
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class StockConcurrencyTest {

    @Autowired
    private StockService stockService;

    // 도메인 레포
    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StockRepository stockRepository;

    // JPA 레포 (초기화/조회 편의용)
    @Autowired
    private StockJpaRepository stockJpaRepository;

    @Autowired
    private StockReservationJpaRepository stockReservationJpaRepository;

    @Test
    @DisplayName("여러 스레드가 동시에 예약해도 총 예약 수량이 실제 재고를 초과하지 않는다")
    void concurrent_reservation_should_not_exceed_available_stock() throws Exception {
        // given
        stockReservationJpaRepository.deleteAll();
        stockJpaRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();

        Category category = Category.create("동시성-카테고리", "동시성 테스트용");
        categoryRepository.save(category);

        UUID companyId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();
        int initialQuantity = 100;

        Product product = Product.create(
                "동시성-상품",
                Money.of(10_000L),
                category.getId(),
                companyId,
                hubId,
                initialQuantity
        );
        productRepository.save(product);

        Stock stock = Stock.create(product.getId(), companyId, hubId, initialQuantity);
        stockJpaRepository.save(stock);

        int totalTasks = 300;      // 시도 횟수 (재고보다 크게)
        int threadPoolSize = 32;   // 동시 스레드 수

        ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize);
        CountDownLatch latch = new CountDownLatch(totalTasks);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failureCount = new AtomicInteger();

        for (int i = 0; i < totalTasks; i++) {
            final int attemptNo = i;
            executor.submit(() -> {
                try {
                    String reservationKey = "resv-" + attemptNo; // 각 시도마다 고유 키
                    stockService.reserveStock(product.getId(), reservationKey, 1);
                    successCount.incrementAndGet();
                } catch (BusinessException ex) {
                    // 재고 부족, 기타 비즈니스 예외는 실패로 카운트
                    failureCount.incrementAndGet();
                } catch (Exception ex) {
                    // 낙관적 락 재시도 실패 등의 예외도 전부 실패로 카운트
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // when
        Stock reloaded = stockRepository.findByProductId(product.getId())
                .orElseThrow(() -> new AssertionError("재고를 찾을 수 없습니다."));

        List<StockReservation> reservations = stockReservationJpaRepository.findAll();

        int reservedQuantitySum = reservations.stream()
                .filter(r -> r.getStockId().equals(reloaded.getId()))
                .mapToInt(StockReservation::getReservedQuantity)
                .sum();

        // then
        // 1) Stock 엔티티의 reservedQuantity와 Reservation 합이 일치해야 한다.
        assertThat(reloaded.getReservedQuantity())
                .as("Stock.reservedQuantity와 Reservation 합계가 일치해야 한다")
                .isEqualTo(reservedQuantitySum);

        // 2) 실제 예약된 수량은 초기 재고를 절대 초과하지 않아야 한다.
        assertThat(reloaded.getReservedQuantity())
                .as("총 예약 수량은 초기 재고를 초과하면 안 된다")
                .isLessThanOrEqualTo(initialQuantity);

        // 3) 성공한 예약 수는 예약된 수량과 같아야 한다. (각 예약은 1개씩)
        assertThat(successCount.get())
                .as("성공한 예약 수는 reservedQuantity와 같아야 한다")
                .isEqualTo(reloaded.getReservedQuantity());

        // 4) 총 시도 횟수 = 성공 + 실패
        assertThat(successCount.get() + failureCount.get())
                .as("성공/실패 카운트 합은 총 시도 횟수와 같아야 한다")
                .isEqualTo(totalTasks);
    }
}
