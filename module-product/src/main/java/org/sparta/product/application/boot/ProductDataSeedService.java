package org.sparta.product.application.boot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.product.domain.entity.Category;
import org.sparta.product.domain.entity.Product;
import org.sparta.product.domain.entity.Stock;
import org.sparta.product.domain.repository.CategoryRepository;
import org.sparta.product.domain.repository.ProductRepository;
import org.sparta.product.domain.repository.StockRepository;
import org.sparta.product.domain.vo.Money;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductDataSeedService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final StockRepository stockRepository;

    private static final int PRODUCTS_PER_CATEGORY = 1250;  // 10,000개 (1250 * 8 카테고리)
    private static final int TOTAL_PRODUCTS = 10000;

    @Transactional
    public void seedCategories() {
        log.info("카테고리 데이터 생성 중...");

        categoryRepository.save(Category.create("전자제품", "노트북, 마우스, 키보드 등"));
        categoryRepository.save(Category.create("식품", "신선식품, 냉동식품, 가공식품 등"));
        categoryRepository.save(Category.create("포장재", "박스, 테이프, 완충재 등"));
        categoryRepository.save(Category.create("의류", "의류, 신발, 액세서리 등"));
        categoryRepository.save(Category.create("생활용품", "세제, 휴지, 청소용품 등"));
        categoryRepository.save(Category.create("도서", "서적, 잡지, 만화책 등"));
        categoryRepository.save(Category.create("스포츠용품", "운동기구, 스포츠웨어 등"));
        categoryRepository.save(Category.create("가구", "책상, 의자, 수납장 등"));

        log.info("카테고리 데이터 생성 완료 (총 8개)");
    }

    @Transactional
    public void seedProductsAndStocks() {
        log.info("상품 및 재고 데이터 생성 중 (목표: {}개)...", TOTAL_PRODUCTS);

        var categories = categoryRepository.findAll();
        UUID dummyCompanyId = UUID.randomUUID();
        UUID dummyHubId = UUID.randomUUID();

        int productCount = 0;

        for (Category category : categories) {
            productCount += seedProductsForCategory(category, dummyCompanyId, dummyHubId);
        }

        log.info("상품 및 재고 데이터 생성 완료 (총 {}개 상품)", productCount);
    }

    private int seedProductsForCategory(Category category, UUID companyId, UUID hubId) {
        String[] productPrefixes = getProductPrefixes(category.getCategoryName());
        int count = 0;

        for (int i = 1; i <= PRODUCTS_PER_CATEGORY; i++) {
            String prefix = productPrefixes[i % productPrefixes.length];
            String productName = String.format("%s %s-%d", category.getCategoryName(), prefix, i);

            long basePrice = getBasePriceByCategory(category.getCategoryName());
            long price = basePrice + (i * 100L);

            int quantity = 10 + (i * 5) % 1000;

            createProductWithStock(productName, Money.of(price), category.getId(), companyId, hubId, quantity);
            count++;

            if (count % 100 == 0) {
                log.info("카테고리 '{}': {}개 상품 생성 완료...", category.getCategoryName(), count);
            }
        }

        return count;
    }

    private String[] getProductPrefixes(String categoryName) {
        return switch (categoryName) {
            case "전자제품" -> new String[]{"노트북", "마우스", "키보드", "모니터", "태블릿", "스피커", "헤드폰", "USB", "SSD", "RAM"};
            case "식품" -> new String[]{"쌀", "라면", "과자", "음료", "냉동식품", "통조림", "즉석밥", "김치", "과일", "채소"};
            case "포장재" -> new String[]{"박스", "테이프", "에어캡", "완충재", "비닐", "끈", "스티커", "포장지", "봉투", "케이스"};
            case "의류" -> new String[]{"티셔츠", "바지", "자켓", "신발", "양말", "모자", "벨트", "장갑", "스카프", "가방"};
            case "생활용품" -> new String[]{"세제", "샴푸", "비누", "칫솔", "수건", "휴지", "세면도구", "청소용품", "쓰레기봉투", "행주"};
            case "도서" -> new String[]{"소설", "만화", "잡지", "참고서", "에세이", "자기계발", "경제경영", "과학", "역사", "예술"};
            case "스포츠용품" -> new String[]{"운동화", "농구공", "축구공", "요가매트", "덤벨", "런닝머신", "자전거", "수영복", "등산복", "텐트"};
            case "가구" -> new String[]{"책상", "의자", "침대", "소파", "책장", "서랍장", "옷장", "테이블", "거울", "조명"};
            default -> new String[]{"상품"};
        };
    }

    private long getBasePriceByCategory(String categoryName) {
        return switch (categoryName) {
            case "전자제품" -> 50000L;
            case "식품" -> 5000L;
            case "포장재" -> 1000L;
            case "의류" -> 20000L;
            case "생활용품" -> 8000L;
            case "도서" -> 15000L;
            case "스포츠용품" -> 30000L;
            case "가구" -> 100000L;
            default -> 10000L;
        };
    }

    private void createProductWithStock(
            String productName,
            Money price,
            UUID categoryId,
            UUID companyId,
            UUID hubId,
            Integer initialQuantity
    ) {
        Product product = Product.create(productName, price, categoryId, companyId, hubId, initialQuantity);
        Product savedProduct = productRepository.save(product);

        Stock stock = Stock.create(savedProduct.getId(), companyId, hubId, initialQuantity);
        stockRepository.save(stock);

        log.debug("상품 생성: {} (재고: {}개)", productName, initialQuantity);
    }
}