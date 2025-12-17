package org.sparta.product.integration;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.sparta.product.application.service.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class StockConcurrencyTest {

    @Autowired
    private StockService stockService;

    @Test
    void concurrent_reservation_idempotent() throws Exception {
        UUID productId = UUID.fromString("00000000-0000-0000-0000-000000000010");
        String reservationKey = "concurrent-key";

        int threads = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        List<Callable<Void>> tasks = IntStream.range(0, threads)
                .mapToObj(i -> (Callable<Void>) () -> {
                    try {
                        stockService.reserveStock(productId, reservationKey, 1);
                    } catch (Exception ignored) {
                    }
                    return null;
                }).toList();

        executor.invokeAll(tasks);
        executor.shutdown();
    }
}
