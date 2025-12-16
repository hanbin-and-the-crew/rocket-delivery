package org.sparta.product.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.sparta.product.application.dto.ProductCreateCommand;
import org.sparta.product.application.dto.ProductDetailInfo;
import org.sparta.product.application.dto.ProductUpdateCommand;
import org.sparta.product.application.service.ProductService;
import org.sparta.product.presentation.dto.product.ProductRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ProductController 통합 + MockMvc 테스트
 * - HTTP 계층에서 JSON <-> DTO 바인딩과 ProductService 호출 여부를 검증
 * - ProductService 는 @MockBean 으로 대체해서 DB/Outbox 로직은 타지 않음
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    @Test
    @DisplayName("상품 생성 API - 성공")
    void createProduct_success() throws Exception {
        // given
        UUID productId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();

        ProductRequest.Create request = new ProductRequest.Create(
                "테스트상품",
                150_000L,
                categoryId,
                companyId,
                hubId,
                100
        );

        ProductDetailInfo detail = new ProductDetailInfo(
                productId,
                request.name(),
                request.price(),
                categoryId,
                companyId,
                hubId,
                request.stock(),
                0,
                true
        );

        given(productService.createProduct(any(ProductCreateCommand.class)))
                .willReturn(productId);
        given(productService.getProduct(productId))
                .willReturn(detail);

        // when
        String json = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                // ApiResponse 구조를 몰라도, 전체 문자열에 id / name 이 포함되는지만 느슨하게 확인
                .andExpect(content().string(containsString(productId.toString())))
                .andExpect(content().string(containsString(request.name())));

        // then - ProductCreateCommand 매핑 검증
        ArgumentCaptor<ProductCreateCommand> captor =
                ArgumentCaptor.forClass(ProductCreateCommand.class);

        then(productService).should(times(1)).createProduct(captor.capture());

        ProductCreateCommand command = captor.getValue();
        assertThat(command.productName()).isEqualTo(request.name());
        assertThat(command.price()).isEqualTo(request.price());
        assertThat(command.categoryId()).isEqualTo(request.categoryId());
        assertThat(command.companyId()).isEqualTo(request.companyId());
        assertThat(command.hubId()).isEqualTo(request.hubId());
        assertThat(command.initialQuantity()).isEqualTo(request.stock());
    }

    @Test
    @DisplayName("상품 단건 조회 API - 성공")
    void getProduct_success() throws Exception {
        // given
        UUID productId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();

        ProductDetailInfo detail = new ProductDetailInfo(
                productId,
                "조회용상품",
                200_000L,
                categoryId,
                companyId,
                hubId,
                50,
                0,
                true
        );

        given(productService.getProduct(productId))
                .willReturn(detail);

        // when
        mockMvc.perform(get("/api/products/{productId}", productId))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(productId.toString())))
                .andExpect(content().string(containsString("조회용상품")))
                .andExpect(content().string(containsString("50"))); // quantity

        // then
        then(productService).should(times(1)).getProduct(productId);
    }

    @Test
    @DisplayName("상품 수정 API - 성공")
    void updateProduct_success() throws Exception {
        // given
        UUID productId = UUID.randomUUID();

        ProductRequest.Update request = new ProductRequest.Update(
                "수정된상품",
                300_000L
        );

        UUID categoryId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();

        ProductDetailInfo detailAfterUpdate = new ProductDetailInfo(
                productId,
                request.name(),
                request.price(),
                categoryId,
                companyId,
                hubId,
                80,
                0,
                true
        );

        // updateProduct 는 void 라서 stubbing 불필요
        given(productService.getProduct(productId))
                .willReturn(detailAfterUpdate);

        // when
        mockMvc.perform(patch("/api/products/{productId}", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(productId.toString())))
                .andExpect(content().string(containsString("수정된상품")))
                .andExpect(content().string(containsString("300000")));

        // then - ProductUpdateCommand 매핑 검증
        ArgumentCaptor<ProductUpdateCommand> captor =
                ArgumentCaptor.forClass(ProductUpdateCommand.class);

        then(productService).should(times(1))
                .updateProduct(eq(productId), captor.capture());

        ProductUpdateCommand command = captor.getValue();
        assertThat(command.productName()).isEqualTo(request.name());
        assertThat(command.price()).isEqualTo(request.price());
        then(productService).should(times(1)).getProduct(productId);
    }

    @Test
    @DisplayName("상품 삭제 API - 성공")
    void deleteProduct_success() throws Exception {
        // given
        UUID productId = UUID.randomUUID();

        // when
        mockMvc.perform(delete("/api/products/{productId}", productId))
                .andExpect(status().isOk());

        // then
        then(productService).should(times(1)).deleteProduct(productId);
    }
}
