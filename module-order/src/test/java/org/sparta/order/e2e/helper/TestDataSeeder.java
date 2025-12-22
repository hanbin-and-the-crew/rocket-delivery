package org.sparta.order.e2e.helper;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * E2E 테스트를 위한 테스트 데이터 생성 헬퍼
 *
 * 실제 외부 서비스의 데이터베이스에 직접 INSERT하여 테스트 데이터를 준비합니다.
 */
public class TestDataSeeder {

    private final String jdbcUrl;
    private final String username;
    private final String password;

    public TestDataSeeder(String jdbcUrl, String username, String password) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, username, password);
    }

    /**
     * 주문 성공 시나리오를 위한 모든 데이터 생성
     */
    public TestData createSuccessScenarioData() {
        UUID customerId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID productId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        UUID couponId = UUID.fromString("55555555-5555-5555-5555-555555555555");
        UUID categoryId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();

        // 1. User 생성
        createUser(customerId, hubId);

        // 2. Point 생성
        createPoint(customerId);

        // 3. Product 생성
        createProduct(productId, categoryId, companyId, hubId);

        // 4. Stock 생성
        createStock(productId, companyId, hubId);

        // 5. Coupon 생성 (선택사항)
        createCoupon(couponId, customerId);

        return new TestData(customerId, productId, couponId, companyId, hubId);
    }

    /**
     * User 데이터 생성
     */
    private void createUser(UUID userId, UUID hubId) {
        String sql = """
            INSERT INTO p_users (user_id, user_name, password, slack_id, real_name,
                                 user_phone_number, email, status, role, hub_id,
                                 created_at, updated_at, deleted_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW(), NULL)
            ON CONFLICT (user_id) DO NOTHING
            """;

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setObject(1, userId);
            pstmt.setString(2, "test_user_" + userId);
            pstmt.setString(3, "$2a$10$N9qo8uLOickgx2ZMRZoMye1C0bBvGK0A8bUHZDQvdQVkWtPqF0BHW");
            pstmt.setString(4, "U01234TEST");
            pstmt.setString(5, "테스트유저");
            pstmt.setString(6, "010-1234-5678");
            pstmt.setString(7, "test_" + userId + "@test.com");
            pstmt.setString(8, "APPROVE");
            pstmt.setString(9, "COMPANY_MANAGER");
            pstmt.setObject(10, hubId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create user", e);
        }
    }

    /**
     * Point 데이터 생성
     */
    private void createPoint(UUID userId) {
        String sql = """
            INSERT INTO p_points (id, user_id, amount, used_amount, reserved_amount,
                                  expiry_date, status, created_at, updated_at, deleted_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), NOW(), NULL)
            ON CONFLICT (id) DO NOTHING
            """;

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setObject(1, UUID.randomUUID());
            pstmt.setObject(2, userId);
            pstmt.setLong(3, 1000000L);
            pstmt.setLong(4, 0L);
            pstmt.setLong(5, 0L);
            pstmt.setObject(6, LocalDateTime.of(2026, 12, 31, 23, 59, 59));
            pstmt.setString(7, "AVAILABLE");
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create point", e);
        }
    }

    /**
     * Product 데이터 생성
     * Product 엔티티는 @Embedded Money를 사용하므로 DB에는 amount 컬럼이 생성됨
     */
    private void createProduct(UUID productId, UUID categoryId, UUID companyId, UUID hubId) {
        String sql = """
            INSERT INTO p_products (id, product_name, amount, category_id, company_id,
                                    hub_id, is_active, created_at, updated_at, deleted_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), NOW(), NULL)
            ON CONFLICT (id) DO NOTHING
            """;

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setObject(1, productId);
            pstmt.setString(2, "테스트 상품");
            pstmt.setLong(3, 10000L); // Money.amount는 Long 타입
            pstmt.setObject(4, categoryId);
            pstmt.setObject(5, companyId);
            pstmt.setObject(6, hubId);
            pstmt.setBoolean(7, true);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create product", e);
        }
    }

    /**
     * Stock 데이터 생성
     */
    private void createStock(UUID productId, UUID companyId, UUID hubId) {
        String sql = """
            INSERT INTO p_stocks (id, product_id, company_id, hub_id, quantity,
                                  reserved_quantity, status, version, created_at, updated_at, deleted_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW(), NULL)
            ON CONFLICT (id) DO NOTHING
            """;

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setObject(1, UUID.randomUUID());
            pstmt.setObject(2, productId);
            pstmt.setObject(3, companyId);
            pstmt.setObject(4, hubId);
            pstmt.setInt(5, 100);
            pstmt.setInt(6, 0);
            pstmt.setString(7, "IN_STOCK");
            pstmt.setLong(8, 0L);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create stock", e);
        }
    }

    /**
     * Coupon 데이터 생성
     */
    private void createCoupon(UUID couponId, UUID userId) {
        String sql = """
            INSERT INTO p_coupons (id, code, name, discount_type, discount_amount,
                                   min_order_amount, start_date, end_date, status,
                                   user_id, order_id, used_at, version, created_at, updated_at, deleted_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NULL, NULL, ?, NOW(), NOW(), NULL)
            """;

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setObject(1, couponId);
            pstmt.setString(2, "TEST_COUPON_" + couponId);
            pstmt.setString(3, "테스트 쿠폰 5000원 할인");
            pstmt.setString(4, "FIXED");
            pstmt.setLong(5, 5000L);
            pstmt.setLong(6, 0L);
            pstmt.setObject(7, LocalDateTime.now().minusDays(1));
            pstmt.setObject(8, LocalDateTime.now().plusDays(30));
            pstmt.setString(9, "AVAILABLE");
            pstmt.setObject(10, userId);
            pstmt.setLong(11, 0L);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create coupon", e);
        }
    }

    /**
     * 모든 테스트 데이터 삭제 (테스트 후 정리)
     */
    public void cleanupAllData() {
        try (Connection conn = getConnection()) {
            executeDelete(conn, "DELETE FROM p_coupons WHERE code LIKE 'TEST_COUPON_%'");
            executeDelete(conn, "DELETE FROM p_stocks WHERE product_id IN (SELECT id FROM p_products WHERE product_name = '테스트 상품')");
            executeDelete(conn, "DELETE FROM p_products WHERE product_name = '테스트 상품'");
            executeDelete(conn, "DELETE FROM p_points WHERE user_id IN (SELECT user_id FROM p_users WHERE user_name LIKE 'test_user_%')");
            executeDelete(conn, "DELETE FROM p_users WHERE user_name LIKE 'test_user_%'");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to cleanup test data", e);
        }
    }

    private void executeDelete(Connection conn, String sql) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.executeUpdate();
        }
    }

    /**
     * 테스트 데이터 홀더
     */
    public record TestData(
        UUID customerId,
        UUID productId,
        UUID couponId,
        UUID companyId,
        UUID hubId
    ) {
    }
}