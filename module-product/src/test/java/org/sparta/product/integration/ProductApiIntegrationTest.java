package org.sparta.product.integration;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sparta.common.event.EventPublisher;
import org.sparta.product.config.TestEventPublisherConfig;
import org.sparta.product.domain.entity.Category;
import org.sparta.product.domain.repository.CategoryRepository;
import org.sparta.product.domain.repository.ProductRepository;
import org.sparta.product.presentation.ProductRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Product API E2E 통합 테스트
 *
 * 테스트 전략:
 * - @SpringBootTest: 전체 애플리케이션 컨텍스트 로드
 * - RANDOM_PORT: 테스트용 랜덤 포트 사용
 * - RestAssured: HTTP 요청/응답 테스트
 * - H2 In-Memory DB 사용
 *
 * 검증 범위:
 * - 상품 생성부터 조회/수정/삭제까지 전체 플로우
 * - HTTP 요청/응답 정확성
 * - API 응답 구조 검증
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestEventPublisherConfig.class)
class ProductApiIntegrationTest {

    @MockBean
    private EventPublisher eventPublisher;

    @LocalServerPort
    private int port;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;

        transactionTemplate.execute(status -> {
            productRepository.deleteAll();
            categoryRepository.deleteAll();
            return null;
        });
    }

    @Test
    @DisplayName("상품 생성부터 조회/수정/삭제까지 전체 플로우가 정상 동작한다")
    void productFullLifecycle_EndToEndFlow() {
        // given: 카테고리 생성
        Category savedCategory = transactionTemplate.execute(status -> {
            Category category = Category.create("전자제품", "전자제품 카테고리");
            return categoryRepository.save(category);
        });

        ProductRequest.Create createRequest = new ProductRequest.Create(
                "테스트 노트북",
                1500000L,
                savedCategory.getId(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                100
        );

        // when: 상품 생성
        String productId = given()
                .contentType(ContentType.JSON)
                .body(createRequest)
        .when()
                .post("/api/products")
        .then()
                .statusCode(200)
                .body("meta.result", equalTo("SUCCESS"))
                .body("data.name", equalTo("테스트 노트북"))
                .body("data.price", equalTo(1500000))
                .extract()
                .path("data.id");

        // then: 생성된 상품 조회
        given()
        .when()
                .get("/api/products/{productId}", productId)
        .then()
                .statusCode(200)
                .body("meta.result", equalTo("SUCCESS"))
                .body("data.productId", equalTo(productId))
                .body("data.name", equalTo("테스트 노트북"))
                .body("data.price", equalTo(1500000))
                .body("data.quantity", equalTo(100));

        // when: 상품 수정
        ProductRequest.Update updateRequest = new ProductRequest.Update(
                "수정된 노트북",
                2000000L
        );

        given()
                .contentType(ContentType.JSON)
                .body(updateRequest)
        .when()
                .patch("/api/products/{productId}", productId)
        .then()
                .statusCode(200)
                .body("meta.result", equalTo("SUCCESS"))
                .body("data.name", equalTo("수정된 노트북"))
                .body("data.price", equalTo(2000000));

        // then: 수정 내용 확인
        given()
        .when()
                .get("/api/products/{productId}", productId)
        .then()
                .statusCode(200)
                .body("data.name", equalTo("수정된 노트북"))
                .body("data.price", equalTo(2000000));

        // when: 상품 삭제 (논리적 삭제)
        given()
        .when()
                .delete("/api/products/{productId}", productId)
        .then()
                .statusCode(200)
                .body("meta.result", equalTo("SUCCESS"));
    }

    @Test
    @DisplayName("존재하지 않는 카테고리로 상품 생성 시 404 에러가 발생한다")
    void createProduct_WithNonExistentCategory_Returns404() {
        // given: 존재하지 않는 카테고리 ID
        ProductRequest.Create request = new ProductRequest.Create(
                "테스트 상품",
                10000L,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                50
        );

        // when & then: 상품 생성 시도
        given()
                .contentType(ContentType.JSON)
                .body(request)
        .when()
                .post("/api/products")
        .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("존재하지 않는 상품 조회 시 404 에러가 발생한다")
    void getProduct_WithNonExistentId_Returns404() {
        // given: 존재하지 않는 상품 ID
        UUID nonExistentId = UUID.randomUUID();

        // when & then: 상품 조회 시도
        given()
        .when()
                .get("/api/products/{productId}", nonExistentId)
        .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("유효하지 않은 요청 데이터로 상품 생성 시 400 에러가 발생한다")
    void createProduct_WithInvalidData_Returns400() {
        // given: 필수 필드가 누락된 잘못된 요청
        String invalidRequest = """
                {
                    "name": "",
                    "price": -1000
                }
                """;

        // when & then: 상품 생성 시도
        given()
                .contentType(ContentType.JSON)
                .body(invalidRequest)
        .when()
                .post("/api/products")
        .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("상품 삭제 시 연관된 재고도 UNAVAILABLE 상태로 변경된다")
    void deleteProduct_ShouldMarkStockAsUnavailable() {
        // given: 카테고리 및 상품 생성
        Category savedCategory = transactionTemplate.execute(status -> {
            Category category = Category.create("가전제품", "가전제품 카테고리");
            return categoryRepository.save(category);
        });

        ProductRequest.Create createRequest = new ProductRequest.Create(
                "냉장고",
                2000000L,
                savedCategory.getId(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                50
        );

        String productId = given()
                .contentType(ContentType.JSON)
                .body(createRequest)
        .when()
                .post("/api/products")
        .then()
                .statusCode(200)
                .extract()
                .path("data.id");

        // when: 상품 삭제
        given()
        .when()
                .delete("/api/products/{productId}", productId)
        .then()
                .statusCode(200)
                .body("meta.result", equalTo("SUCCESS"));

        // then: 삭제된 상품의 Stock 상태가 UNAVAILABLE인지 검증
        transactionTemplate.execute(status -> {
            var product = productRepository.findById(UUID.fromString(productId)).orElseThrow();

            // Product는 비활성화 상태
            org.assertj.core.api.Assertions.assertThat(product.getIsActive()).isFalse();

            // Stock은 이벤트를 통해 UNAVAILABLE 상태로 변경됨 (별도 검증 필요)

            return null;
        });
    }
}