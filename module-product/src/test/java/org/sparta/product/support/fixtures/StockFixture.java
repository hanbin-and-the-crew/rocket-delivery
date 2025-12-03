//package org.sparta.product.support.fixtures;
//
//import org.sparta.product.domain.entity.Stock;
//
//import java.util.UUID;
//
///**
// * Stock 테스트 픽스처
// * - Object Mother 패턴 사용
// * - 반복적으로 사용되는 Stock 객체 생성
// * - 테스트 일관성을 위해 고정된 UUID 사용
// */
//public final class StockFixture {
//
//    // 테스트용 고정 UUID
//    private static final UUID DEFAULT_PRODUCT_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
//    private static final UUID DEFAULT_COMPANY_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
//    private static final UUID DEFAULT_HUB_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
//
//    private StockFixture() {
//    }
//
//    /**
//     * 기본 재고 생성
//     */
//    public static Stock defaultStock() {
//        return Stock.create(
//                DEFAULT_PRODUCT_ID,
//                DEFAULT_COMPANY_ID,
//                DEFAULT_HUB_ID,
//                100
//        );
//    }
//
//    /**
//     * 특정 수량의 재고 생성
//     */
//    public static Stock withQuantity(Integer quantity) {
//        return Stock.create(
//                DEFAULT_PRODUCT_ID,
//                DEFAULT_COMPANY_ID,
//                DEFAULT_HUB_ID,
//                quantity
//        );
//    }
//
//    /**
//     * 특정 Product ID를 가진 재고 생성
//     */
//    public static Stock withProductId(UUID productId) {
//        return Stock.create(
//                productId,
//                DEFAULT_COMPANY_ID,
//                DEFAULT_HUB_ID,
//                100
//        );
//    }
//
//    /**
//     * 특정 Product ID와 수량을 가진 재고 생성
//     */
//    public static Stock withProductIdAndQuantity(UUID productId, Integer quantity) {
//        return withProductIdAndQuantity(productId, DEFAULT_COMPANY_ID, DEFAULT_HUB_ID, quantity);
//    }
//
//    /**
//     * 특정 Product ID, Company ID, Hub ID, 수량을 가진 재고 생성
//     */
//    public static Stock withProductIdAndQuantity(UUID productId, UUID companyId, UUID hubId, Integer quantity) {
//        return Stock.create(
//                productId,
//                companyId,
//                hubId,
//                quantity
//        );
//    }
//
//    /**
//     * 특정 업체의 재고 생성
//     */
//    public static Stock withCompany(UUID companyId) {
//        return Stock.create(
//                DEFAULT_PRODUCT_ID,
//                companyId,
//                DEFAULT_HUB_ID,
//                100
//        );
//    }
//
//    /**
//     * 특정 허브의 재고 생성
//     */
//    public static Stock withHub(UUID hubId) {
//        return Stock.create(
//                DEFAULT_PRODUCT_ID,
//                DEFAULT_COMPANY_ID,
//                hubId,
//                100
//        );
//    }
//
//    /**
//     * 재고 0인 Stock 생성
//     */
//    public static Stock outOfStock() {
//        return Stock.create(
//                DEFAULT_PRODUCT_ID,
//                DEFAULT_COMPANY_ID,
//                DEFAULT_HUB_ID,
//                0
//        );
//    }
//
//    /**
//     * 모두 예약된 재고 생성
//     * - 재고 100개, 예약 100개
//     */
//    public static Stock fullyReserved() {
//        Stock stock = withQuantity(100);
//        stock.reserve(100);
//        return stock;
//    }
//
//    /**
//     * 일부 예약된 재고 생성
//     * - 재고 100개, 예약 30개
//     */
//    public static Stock partiallyReserved() {
//        Stock stock = withQuantity(100);
//        stock.reserve(30);
//        return stock;
//    }
//
//    /**
//     * 판매 불가 상태의 재고 생성
//     */
//    public static Stock unavailable() {
//        Stock stock = defaultStock();
//        stock.markAsUnavailable();
//        return stock;
//    }
//}