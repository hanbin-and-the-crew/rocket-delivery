package org.sparta.product.support;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.sparta.product.domain.entity.Category;
import org.sparta.product.domain.entity.Product;
import org.sparta.product.domain.entity.Stock;
import org.sparta.product.domain.repository.CategoryRepository;
import org.sparta.product.domain.repository.ProductRepository;
import org.sparta.product.domain.repository.StockRepository;
import org.sparta.product.domain.vo.Money;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
public class ProductSeedInitializer {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final StockRepository stockRepository;

    @PostConstruct
    public void init() {
        log.info("[INIT] ProductSeedInitializer 시작");

        // 이미 Category가 있다면 Seed 데이터가 존재한다고 간주 → 스킵
        if (categoryRepository.count() > 0) {
            log.info("[INIT] 기존 Category 데이터 존재 → Seed 생성 스킵");
            return;
        }

        // 1. 카테고리 생성
        log.info("[INIT] Category 생성 시작");
        UUID category1 = createCategory("신선식품", "신선 과일/채소");
        UUID category2 = createCategory("냉동식품", "육류/해산물 냉동");
        UUID category3 = createCategory("가공식품", "과자/통조림/음료");

        List<UUID> categories = List.of(category1, category2, category3);
        log.info("[INIT] Category 생성 완료");

        // 공통 테스트용 companyId / hubId
        UUID companyId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID hubId = UUID.fromString("22222222-2222-2222-2222-222222222222");

        // 2. Product + Stock 생성
        for (int i = 1; i <= 10; i++) {

            UUID categoryId = categories.get(i % categories.size());

            Product product = Product.create(
                    "테스트상품 " + i,
                    Money.of(1000L * i),  // Long 타입 적용
                    categoryId,
                    companyId,
                    hubId,
                    50                 // initial quantity 검증용
            );

            productRepository.save(product);

            Stock stock = Stock.create(
                    product.getId(),
                    companyId,
                    hubId,
                    50 + i     // 실제 재고
            );

            stockRepository.save(stock);

            log.info("[INIT] Product/Stock 생성 완료: {}", product.getProductName());
        }

        log.info("[INIT] Product/Stock Seed 데이터 생성 완료");
    }

    private UUID createCategory(String name, String description) {
        Category category = Category.create(name, description);
        return categoryRepository.save(category).getId();
    }
}
