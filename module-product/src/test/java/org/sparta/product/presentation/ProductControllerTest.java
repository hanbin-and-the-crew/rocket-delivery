package org.sparta.product.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sparta.common.api.ApiControllerAdvice;
import org.sparta.common.error.BusinessException;
import org.sparta.product.domain.error.ProductErrorType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.sparta.product.application.service.ProductService;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ProductController 단위 테스트
 *
 * 테스트 전략:
 * - @WebMvcTest: Controller 레이어만 로드
 * - MockMvc: HTTP 요청/응답 테스트
 * - @MockBean: ProductService Mock
 * - @Import: ApiControllerAdvice 로드하여 예외 처리 테스트
 *
 * TDD 방식으로 API 정의 후 구현
 */
@WebMvcTest(ProductController.class)
@Import(ApiControllerAdvice.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    @Test
    @DisplayName("상품 생성 API - 성공")
    void createProduct_ShouldReturnSuccess() throws Exception {
        // given
        ProductRequest.Create request = new ProductRequest.Create(
                "테스트 상품",
                10000L,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                100
        );

        ProductResponse.Create response = new ProductResponse.Create(
                UUID.randomUUID(),
                "테스트 상품",
                10000L
        );

        given(productService.createProduct(any(ProductRequest.Create.class)))
                .willReturn(response);

        // when & then
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.name").value("테스트 상품"))
                .andExpect(jsonPath("$.data.price").value(10000));
    }

    @Test
    @DisplayName("존재하지 않는 카테고리로 생성 시 404 에러 반환")
    void createProduct_WithNonExistentCategory_ShouldReturnError() throws Exception {
        // given
        ProductRequest.Create request = new ProductRequest.Create(
                "테스트 상품",
                10000L,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                100
        );

        given(productService.createProduct(any(ProductRequest.Create.class)))
                .willThrow(new BusinessException(ProductErrorType.PRODUCT_NOT_FOUND));

        // when & then
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("상품 단 건 조회 API - 성공")
    void getProduct_ShouldReturnProduct() throws Exception {
        // given
        UUID productId = UUID.randomUUID();
        ProductResponse.Detail response = new ProductResponse.Detail(
                productId,
                "테스트 상품",
                10000L,
                100
        );

        given(productService.getProduct(productId))
                .willReturn(response);

        // when & then
        mockMvc.perform(get("/api/products/{productId}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.productId").value(productId.toString()))
                .andExpect(jsonPath("$.data.name").value("테스트 상품"))
                .andExpect(jsonPath("$.data.quantity").value(100));
    }

    @Test
    @DisplayName("존재하지 않는 상품 조회 시 404 에러 반환 ")
    void getProduct_WithNonExistentId_ShouldReturnNotFound() throws Exception {
        // given
        UUID productId = UUID.randomUUID();

        given(productService.getProduct(productId))
                .willThrow(new BusinessException(ProductErrorType.PRODUCT_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/api/products/{productId}", productId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("상품 정보 수정 API - 성공")
    void updateProduct_ShouldReturnSuccess() throws Exception {
        // given
        UUID productId = UUID.randomUUID();
        ProductRequest.Update request = new ProductRequest.Update(
                "수정된 상품명",
                15000L
        );

        ProductResponse.Update response = new ProductResponse.Update(
                productId,
                "수정된 상품명",
                15000L
        );

        given(productService.updateProduct(any(UUID.class), any(ProductRequest.Update.class)))
                .willReturn(response);

        // when & then
        mockMvc.perform(patch("/api/products/{productId}", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.name").value("수정된 상품명"))
                .andExpect(jsonPath("$.data.price").value(15000));
    }

    @Test
    @DisplayName("상품 소프트 삭제 API - 성공")
    void deleteProduct_ShouldReturnSuccess() throws Exception {
        // given
        UUID productId = UUID.randomUUID();

        // when & then
        mockMvc.perform(delete("/api/products/{productId}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.result").value("SUCCESS"));
    }

    @Test
    @DisplayName("이미 삭제 된 상품 또는 존재하지 ㅇ낳는 상 존재하지 않는 상품 삭제")
    void deleteProduct_WithNonExistentId_ShouldReturnNotFound() throws Exception {
        // given
        UUID productId = UUID.randomUUID();

        doThrow(new BusinessException(ProductErrorType.PRODUCT_NOT_FOUND))
                .when(productService).deleteProduct(productId);

        // when & then
        mockMvc.perform(delete("/api/products/{productId}", productId))
                .andExpect(status().isNotFound());
    }
}