//package org.sparta.product.integration;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.sparta.common.event.EventPublisher;
//import org.sparta.product.application.service.StockService;
//import org.sparta.product.domain.entity.Category;
//import org.sparta.product.domain.entity.Product;
//import org.sparta.product.domain.entity.Stock;
//import org.sparta.product.domain.repository.CategoryRepository;
//import org.sparta.product.domain.repository.ProductRepository;
//import org.sparta.product.domain.repository.StockRepository;
//import org.sparta.product.support.fixtures.ProductFixture;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.context.annotation.Import;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.transaction.support.TransactionTemplate;
//
//import java.util.UUID;
//import java.util.concurrent.CountDownLatch;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.atomic.AtomicInteger;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//@SpringBootTest
//@ActiveProfiles("test")
//@DisplayName("재고 동시성 처리 통합 테스트 (Fixture 적용)")
//class StockConcurrencyTest {
//
//    @MockBean
//    private EventPublisher eventPublisher;
//
//    @Autowired
//    private StockService stockService;
//
//    @Autowired
//    private ProductRepository productRepository;
//
//    @Autowired
//    private StockRepository stockRepository;
//
//    @Autowired
//    private CategoryRepository categoryRepository;
//
//    @Autowired
//    private TransactionTemplate transactionTemplate;
//
//    @BeforeEach
//    void setUp() {
//        transactionTemplate.execute(status -> {
//            productRepository.deleteAll();
//            categoryRepository.deleteAll();
//            return null;
//        });
//    }
//
//    private UUID saveProductWithStock(int initialQuantity) {
//        Product product = ProductFixture.withStock(initialQuantity);
//        return transactionTemplate.execute(status -> {
//            Category category = categoryRepository.save(Category.create("전자제품", "테스트 카테고리"));
//            Product savedProduct = productRepository.save(
//                    Product.create(
//                            product.getProductName(),
//                            product.getPrice(),
//                            category.getId(),
//                            product.getCompanyId(),
//                            product.getHubId(),
//                            initialQuantity
//                    )
//            );
//            stockRepository.save(
//                    Stock.create(
//                            savedProduct.getId(),
//                            savedProduct.getCompanyId(),
//                            savedProduct.getHubId(),
//                            initialQuantity
//                    )
//            );
//            return savedProduct.getId();
//        });
//    }
//
//    @Test
//    @DisplayName("동시에 100개 예약 요청이 들어와도 재고가 정확히 차감된다")
//    void concurrentReservation_ShouldMaintainDataConsistency() throws InterruptedException {
//        UUID productId = saveProductWithStock(100);
//
//        int numberOfThreads = 100;
//        int reserveQuantityPerThread = 1;
//        ExecutorService executorService = Executors.newFixedThreadPool(10);
//        CountDownLatch latch = new CountDownLatch(numberOfThreads);
//        AtomicInteger successCount = new AtomicInteger(0);
//        AtomicInteger failCount = new AtomicInteger(0);
//
//        for (int i = 0; i < numberOfThreads; i++) {
//            executorService.execute(() -> {
//                try {
//                    stockService.reserveStock(productId, reserveQuantityPerThread);
//                    successCount.incrementAndGet();
//                } catch (Exception e) {
//                    failCount.incrementAndGet();
//                } finally {
//                    latch.countDown();
//                }
//            });
//        }
//
//        latch.await();
//        executorService.shutdown();
//
//        Stock stock = stockRepository.findByProductId(productId).orElseThrow();
//        assertThat(successCount.get()).isEqualTo(100);
//        assertThat(failCount.get()).isEqualTo(0);
//        assertThat(stock.getReservedQuantity()).isEqualTo(100);
//        assertThat(stock.getAvailableQuantity()).isEqualTo(0);
//    }
//
//    @Test
//    @DisplayName("재고보다 많은 동시 예약 요청이 들어오면 일부만 성공한다")
//    void concurrentReservation_WithInsufficientStock_ShouldPartiallySucceed() throws InterruptedException {
//        UUID productId = saveProductWithStock(50);
//
//        int numberOfThreads = 100;
//        int reserveQuantityPerThread = 1;
//        ExecutorService executorService = Executors.newFixedThreadPool(10);
//        CountDownLatch latch = new CountDownLatch(numberOfThreads);
//        AtomicInteger successCount = new AtomicInteger(0);
//        AtomicInteger failCount = new AtomicInteger(0);
//
//        for (int i = 0; i < numberOfThreads; i++) {
//            executorService.execute(() -> {
//                try {
//                    stockService.reserveStock(productId, reserveQuantityPerThread);
//                    successCount.incrementAndGet();
//                } catch (Exception e) {
//                    failCount.incrementAndGet();
//                } finally {
//                    latch.countDown();
//                }
//            });
//        }
//
//        latch.await();
//        executorService.shutdown();
//
//        Stock stock = stockRepository.findByProductId(productId).orElseThrow();
//        assertThat(successCount.get()).isEqualTo(50);
//        assertThat(failCount.get()).isEqualTo(50);
//        assertThat(stock.getReservedQuantity()).isEqualTo(50);
//        assertThat(stock.getAvailableQuantity()).isEqualTo(0);
//    }
//
//    @Test
//    @DisplayName("동시 예약 확정 요청도 정확히 처리된다")
//    void concurrentConfirmation_ShouldMaintainDataConsistency() throws InterruptedException {
//        UUID productId = saveProductWithStock(100);
//
//        // 100개 예약
//        transactionTemplate.execute(status -> {
//            Stock stock = stockRepository.findByProductId(productId).orElseThrow();
//            for (int i = 0; i < 100; i++) {
//                stock.reserve(1);
//            }
//            stockRepository.save(stock);
//            return null;
//        });
//
//        int numberOfThreads = 100;
//        int confirmQuantityPerThread = 1;
//        ExecutorService executorService = Executors.newFixedThreadPool(10);
//        CountDownLatch latch = new CountDownLatch(numberOfThreads);
//        AtomicInteger successCount = new AtomicInteger(0);
//        AtomicInteger failCount = new AtomicInteger(0);
//
//        for (int i = 0; i < numberOfThreads; i++) {
//            executorService.execute(() -> {
//                try {
//                    stockService.confirmReservation(productId, confirmQuantityPerThread);
//                    successCount.incrementAndGet();
//                } catch (Exception e) {
//                    failCount.incrementAndGet();
//                } finally {
//                    latch.countDown();
//                }
//            });
//        }
//
//        latch.await();
//        executorService.shutdown();
//
//        Stock stock = stockRepository.findByProductId(productId).orElseThrow();
//        assertThat(successCount.get()).isEqualTo(100);
//        assertThat(failCount.get()).isEqualTo(0);
//        assertThat(stock.getQuantity()).isEqualTo(0);
//        assertThat(stock.getReservedQuantity()).isEqualTo(0);
//    }
//
//    private void retryOnOptimisticLock(Runnable task, int maxRetries) {
//        int attempt = 0;
//        while (attempt < maxRetries) {
//            try {
//                task.run();
//                return;
//            } catch (org.springframework.orm.ObjectOptimisticLockingFailureException |
//                     jakarta.persistence.OptimisticLockException e) {
//                attempt++;
//                if (attempt >= maxRetries) {
//                    throw e;
//                }
//                try {
//                    Thread.sleep(10);
//                } catch (InterruptedException ie) {
//                    Thread.currentThread().interrupt();
//                    throw new RuntimeException(ie);
//                }
//            }
//        }
//    }
//}
