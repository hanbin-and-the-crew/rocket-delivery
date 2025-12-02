package org.sparta.product.application.boot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.product.domain.repository.CategoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile({"local", "dev"})
@RequiredArgsConstructor
public class ProductDataSeeder implements CommandLineRunner {

    private final ProductDataSeedService seedService;
    private final CategoryRepository categoryRepository;

    @Override
    public void run(String... args) {
        long categoryCount = categoryRepository.count();
        log.info("현재 카테고리 수: {}", categoryCount);

        if (categoryCount > 0) {
            log.info("초기 데이터가 이미 존재하여 데이터 시딩을 건너뜁니다.");
            return;
        }

        log.info("Product 모듈 초기 데이터 시딩을 시작합니다...");

        seedService.seedCategories();
        seedService.seedProductsAndStocks();

        log.info("Product 모듈 초기 데이터 시딩이 완료되었습니다.");
    }
}