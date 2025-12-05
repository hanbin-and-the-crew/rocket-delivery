//package org.sparta.order.application.presentation;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.github.tomakehurst.wiremock.WireMockServer;
//import org.junit.jupiter.api.*;
//import org.sparta.order.OrderApplication;
//import org.sparta.order.presentation.dto.request.OrderRequest;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.http.MediaType;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.test.context.DynamicPropertyRegistry;
//import org.springframework.test.context.DynamicPropertySource;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
//
//import java.time.LocalDateTime;
//import java.util.UUID;
//
//import static com.github.tomakehurst.wiremock.client.WireMock.*;
//import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
//@SpringBootTest(
//        classes = OrderApplication.class,
//        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
//)
//@AutoConfigureMockMvc
//@ActiveProfiles("test")
//class OrderHttpIntegrationTest {
//
//    @Autowired
//    MockMvc mockMvc;
//
//    @Autowired
//    ObjectMapper objectMapper;
//
//    // 외부 서비스 WireMock
//    static WireMockServer productServer = new WireMockServer(wireMockConfig().dynamicPort());
//    static WireMockServer userServer = new WireMockServer(wireMockConfig().dynamicPort());
//    static WireMockServer couponServer = new WireMockServer(wireMockConfig().dynamicPort());
//    static WireMockServer paymentServer = new WireMockServer(wireMockConfig().dynamicPort());
//
//    @DynamicPropertySource
//    static void overrideFeignUrls(DynamicPropertyRegistry registry) {
//
//        registry.add("spring.cloud.openfeign.client.config.product-service.url",
//                () -> productServer.baseUrl());
//
//        registry.add("spring.cloud.openfeign.client.config.user-service.url",
//                () -> userServer.baseUrl());
//
//        registry.add("spring.cloud.openfeign.client.config.coupon-service.url",
//                () -> couponServer.baseUrl());
//
//        registry.add("spring.cloud.openfeign.client.config.payment-service.url",
//                () -> paymentServer.baseUrl());
//
//        registry.add("eureka.client.enabled", () -> false);
//    }
//
//    @BeforeAll
//    static void setUp() {
//        productServer.start();
//        userServer.start();
//        couponServer.start();
//        paymentServer.start();
//    }
//
//    @AfterAll
//    static void tearDown() {
//        productServer.stop();
//        userServer.stop();
//        couponServer.stop();
//        paymentServer.stop();
//    }
//
//    @Test
//    void createOrder_success_httpIntegration() throws Exception {
//
//        // ====== Request 생성 ======
//        UUID customerId = UUID.randomUUID();
//        UUID supplierCompanyId = UUID.randomUUID();
//        UUID supplierHubId = UUID.randomUUID();
//        UUID receiptCompanyId = UUID.randomUUID();
//        UUID receiptHubId = UUID.randomUUID();
//        UUID productId = UUID.randomUUID();
//        UUID couponId = UUID.randomUUID();
//
//        OrderRequest.Create req = new OrderRequest.Create(
//                supplierCompanyId,
//                supplierHubId,
//                receiptCompanyId,
//                receiptHubId,
//                productId,
//                3,
//                10000,
//                "서울 강남구 123",
//                "홍길동",
//                "01012341234",
//                "abc@exam.com",
//                LocalDateTime.now().plusDays(1),
//                "빠른 배송",
//                2000,
//                couponId.toString(),
//                "CARD",
//                "TOSS",
//                "KRW"
//        );
//
//        String body = objectMapper.writeValueAsString(req);
//
//        // ============= WireMock Stub 시작 =============
//
//        // 1. stock reserve
//        productServer.stubFor(
//                post(urlEqualTo("/product/stocks/reserve"))
//                        .willReturn(aResponse()
//                                .withStatus(200)
//                                .withHeader("Content-Type", "application/json")
//                                .withBody("""
//                                        {
//                                          "reservationId": "%s",
//                                          "stockId": "%s",
//                                          "reservationKey": "test-key",
//                                          "reservedQuantity": 3,
//                                          "status": "RESERVED"
//                                        }
//                                        """.formatted(UUID.randomUUID(), UUID.randomUUID()))
//                        )
//        );
//
//        // 2. point reserve
//        userServer.stubFor(
//                post(urlEqualTo("/users/point/reserve"))
//                        .willReturn(aResponse()
//                                .withStatus(200)
//                                .withHeader("Content-Type", "application/json")
//                                .withBody("""
//                                        {"reservationId":"%s"}
//                                        """.formatted(UUID.randomUUID()))
//                        )
//        );
//
//        // 3. coupon reserve
//        couponServer.stubFor(
//                post(urlPathMatching("/api/coupons/.*/reserve"))
//                        .willReturn(aResponse()
//                                .withStatus(200)
//                                .withHeader("Content-Type", "application/json")
//                                .withBody("""
//                                        {
//                                          "valid": true,
//                                          "reservationId": "%s",
//                                          "discountAmount": 3000,
//                                          "discountType": "FIXED",
//                                          "expiresAt": "2030-12-31T23:59:59"
//                                        }
//                                        """.formatted(UUID.randomUUID()))
//                        )
//        );
//
//        // 4. payment approve
//        paymentServer.stubFor(
//                post(urlPathMatching("/api/payment/.*/approve"))
//                        .willReturn(aResponse()
//                                .withStatus(200)
//                                .withHeader("Content-Type", "application/json")
//                                .withBody("""
//                                        {"pgToken":"pg_12345"}
//                                        """))
//        );
//
//        // ============= MockMvc 호출 =============
//        var result = mockMvc.perform(
//                        MockMvcRequestBuilders.post("/api/orders")
//                                .header("X-USER-ID", customerId.toString())
//                                .contentType(MediaType.APPLICATION_JSON)
//                                .content(body)
//                )
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.meta.result").value("SUCCESS"))
//                .andExpect(jsonPath("$.data.orderId").exists())
//                .andReturn();
//
////        System.out.println(result.getResponse().getContentAsString());
//
//        // FeignClient 호출 검증 추가
//        productServer.verify(1, postRequestedFor(urlEqualTo("/product/stocks/reserve")));
//        userServer.verify(1, postRequestedFor(urlEqualTo("/users/point/reserve")));
//        couponServer.verify(1, postRequestedFor(urlPathMatching("/api/coupons/.*/reserve")));
//        paymentServer.verify(1, postRequestedFor(urlPathMatching("/api/payment/.*/approve")));
//    }
//}
