package org.sparta.product.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sparta.product.application.service.StockService;
import org.sparta.product.domain.entity.Category;
import org.sparta.product.domain.entity.Product;
import org.sparta.product.domain.repository.CategoryRepository;
import org.sparta.product.domain.repository.ProductRepository;
import org.sparta.product.domain.vo.Money;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 재고 동시성 처리 통합 테스트
 *
 * Step 5: 복잡한 비즈니스 로직 - 동시성 제어
 *
 * 테스트 전략:
 * - Testcontainers: 실제 PostgreSQL 컨테이너 사용
 * - @SpringBootTest: 실제 스프링 컨텍스트와 DB 사용
 * - ExecutorService: 동시 요청 시뮬레이션
 * - JPA @Version + @Retryable: 낙관적 락으로 동시성 제어 및 재시도
 *
 * 검증 사항:
 * 1. 동시에 100개 예약 요청이 들어와도 재고가 정확히 차감되는지
 * 2. 낙관적 락 충돌 시 @Retryable로 자동 재시도되는지
 * 3. 재고 부족 시 일부만 성공하는지
 */
@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("재고 동시성 처리 통합 테스트")
class StockConcurrencyTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("test_db")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private StockService stockService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        transactionTemplate.execute(status -> {
            productRepository.deleteAll();
            categoryRepository.deleteAll();
            return null;
        });
    }

    @Test
    @DisplayName("동시에 100개 예약 요청이 들어와도 재고가 정확히 차감된다")
    void concurrentReservation_ShouldMaintainDataConsistency() throws InterruptedException {
        // given: 재고 100개인 상품 생성
        UUID productId = transactionTemplate.execute(status -> {
            Category category = Category.create("전자제품", "테스트 카테고리");
            categoryRepository.save(category);

            Product product = Product.create(
                    "동시성 테스트 상품",
                    Money.of(10000L),
                    category.getId(),
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    100 // 재고 100개
            );
            return productRepository.save(product).getId();
        });

        int numberOfThreads = 100;
        int reserveQuantityPerThread = 1;
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when: 100개 스레드에서 동시에 1개씩 예약
        for (int i = 0; i < numberOfThreads; i++) {
            executorService.execute(() -> {
                try {
                    // 낙관적 락 충돌 시 재시도 (최대 3회)
                    retryOnOptimisticLock(() -> {
                        transactionTemplate.execute(status -> {
                            stockService.reserveStock(productId, reserveQuantityPerThread);
                            return null;
                        });
                    }, 3);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then: 100개 모두 성공, 예약 재고 100개
        Product product = productRepository.findById(productId).orElseThrow();
        assertThat(successCount.get()).isEqualTo(100);
        assertThat(failCount.get()).isEqualTo(0);
        assertThat(product.getStock().getReservedQuantity()).isEqualTo(100);
        assertThat(product.getStock().getAvailableQuantity()).isEqualTo(0);
    }

    @Test
    @DisplayName("재고보다 많은 동시 예약 요청이 들어오면 일부만 성공한다")
    void concurrentReservation_WithInsufficientStock_ShouldPartiallySucceed() throws InterruptedException {
        // given: 재고 50개인 상품 생성
        UUID productId = transactionTemplate.execute(status -> {
            Category category = Category.create("전자제품", "테스트 카테고리");
            categoryRepository.save(category);

            Product product = Product.create(
                    "재고 부족 테스트 상품",
                    Money.of(10000L),
                    category.getId(),
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    50 // 재고 50개
            );
            return productRepository.save(product).getId();
        });

        int numberOfThreads = 100;
        int reserveQuantityPerThread = 1;
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when: 100개 스레드에서 동시에 1개씩 예약 (재고는 50개만)
        for (int i = 0; i < numberOfThreads; i++) {
            executorService.execute(() -> {
                try {
                    transactionTemplate.execute(status -> {
                        stockService.reserveStock(productId, reserveQuantityPerThread);
                        return null;
                    });
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then: 50개만 성공, 50개는 실패
        Product product = productRepository.findById(productId).orElseThrow();
        assertThat(successCount.get()).isEqualTo(50);
        assertThat(failCount.get()).isEqualTo(50);
        assertThat(product.getStock().getReservedQuantity()).isEqualTo(50);
        assertThat(product.getStock().getAvailableQuantity()).isEqualTo(0);
    }

    @Test
    @DisplayName("동시 예약 확정 요청도 정확히 처리된다")
    void concurrentConfirmation_ShouldMaintainDataConsistency() throws InterruptedException {
        // given: 재고 100개, 100개 예약된 상품 생성
        UUID productId = transactionTemplate.execute(status -> {
            Category category = Category.create("전자제품", "테스트 카테고리");
            categoryRepository.save(category);

            Product product = Product.create(
                    "예약 확정 테스트 상품",
                    Money.of(10000L),
                    category.getId(),
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    100
            );
            product = productRepository.save(product);

            // 100개 예약
            for (int i = 0; i < 100; i++) {
                product.getStock().reserve(1);
            }
            productRepository.save(product);

            return product.getId();
        });

        int numberOfThreads = 100;
        int confirmQuantityPerThread = 1;
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when: 100개 스레드에서 동시에 1개씩 확정
        for (int i = 0; i < numberOfThreads; i++) {
            executorService.execute(() -> {
                try {
                    transactionTemplate.execute(status -> {
                        stockService.confirmReservation(productId, confirmQuantityPerThread);
                        return null;
                    });
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then: 100개 모두 성공, 실제 재고 0, 예약 재고 0
        Product product = productRepository.findById(productId).orElseThrow();
        assertThat(successCount.get()).isEqualTo(100);
        assertThat(failCount.get()).isEqualTo(0);
        assertThat(product.getStock().getQuantity()).isEqualTo(0);
        assertThat(product.getStock().getReservedQuantity()).isEqualTo(0);
    }

    /**
     * 낙관적 락 충돌 시 재시도 헬퍼 메서드
     */
    private void retryOnOptimisticLock(Runnable task, int maxRetries) {
        int attempt = 0;
        while (attempt < maxRetries) {
            try {
                task.run();
                return; // 성공 시 종료
            } catch (org.springframework.orm.ObjectOptimisticLockingFailureException |
                     jakarta.persistence.OptimisticLockException e) {
                attempt++;
                if (attempt >= maxRetries) {
                    throw e; // 최대 재시도 횟수 초과 시 예외 던지기
                }
                try {
                    Thread.sleep(10); // 짧은 대기 후 재시도
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(ie);
                }
            }
        }
    }
}