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
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @MockBean
    private ProductService productService;

    // @EnableJpaAuditing 환경에서 WebMvc slice에 metamodel 빈이 필요할 수 있음
    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @jakarta.annotation.Resource
    private MockMvc mockMvc;

    @jakarta.annotation.Resource
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/products - 유효 요청이면 create 호출 + 200")
    void create_success() throws Exception {
        UUID categoryId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID companyId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID hubId = UUID.fromString("00000000-0000-0000-0000-000000000003");
        UUID productId = UUID.fromString("00000000-0000-0000-0000-000000000004");

        ProductRequest.Create request = new ProductRequest.Create(
                "노트북",
                1500000L,
                categoryId,
                companyId,
                hubId,
                100
        );

        when(productService.createProduct(any(ProductCreateCommand.class))).thenReturn(productId);
        when(productService.getProduct(productId)).thenReturn(mock(ProductDetailInfo.class));

        mockMvc.perform(
                        post("/api/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk());

        ArgumentCaptor<ProductCreateCommand> captor = ArgumentCaptor.forClass(ProductCreateCommand.class);
        verify(productService).createProduct(captor.capture());

        ProductCreateCommand cmd = captor.getValue();
        assertThat(cmd.productName()).isEqualTo("노트북");
        assertThat(cmd.price()).isEqualTo(1500000L);
        assertThat(cmd.categoryId()).isEqualTo(categoryId);
        assertThat(cmd.companyId()).isEqualTo(companyId);
        assertThat(cmd.hubId()).isEqualTo(hubId);
        assertThat(cmd.initialQuantity()).isEqualTo(100);

        verify(productService).getProduct(productId);
        verifyNoMoreInteractions(productService);
    }

    @Test
    @DisplayName("POST /api/products - validation 실패면 400 + service 미호출")
    void create_validationFail_400() throws Exception {
        UUID categoryId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID companyId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID hubId = UUID.fromString("00000000-0000-0000-0000-000000000003");

        ProductRequest.Create invalid = new ProductRequest.Create(
                "",          // @NotBlank
                -1L,         // @Positive
                categoryId,   // @NotNull
                companyId,    // @NotNull
                hubId,        // @NotNull
                0             // @Positive
        );

        mockMvc.perform(
                        post("/api/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalid))
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(productService);
    }

    @Test
    @DisplayName("GET /api/products/{productId} - service 호출 + 200")
    void get_success() throws Exception {
        UUID productId = UUID.fromString("00000000-0000-0000-0000-000000000004");

        when(productService.getProduct(productId)).thenReturn(mock(ProductDetailInfo.class));

        mockMvc.perform(get("/api/products/{productId}", productId))
                .andExpect(status().isOk());

        verify(productService).getProduct(productId);
        verifyNoMoreInteractions(productService);
    }

    @Test
    @DisplayName("PATCH /api/products/{productId} - 유효 요청이면 update 호출 + 200")
    void update_success() throws Exception {
        UUID productId = UUID.fromString("00000000-0000-0000-0000-000000000004");

        ProductRequest.Update request = new ProductRequest.Update("수정명", 2000000L);

        doNothing().when(productService).updateProduct(eq(productId), any(ProductUpdateCommand.class));
        when(productService.getProduct(productId)).thenReturn(mock(ProductDetailInfo.class));

        mockMvc.perform(
                        patch("/api/products/{productId}", productId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk());

        ArgumentCaptor<ProductUpdateCommand> captor = ArgumentCaptor.forClass(ProductUpdateCommand.class);
        verify(productService).updateProduct(eq(productId), captor.capture());

        ProductUpdateCommand cmd = captor.getValue();
        assertThat(cmd.productName()).isEqualTo("수정명");
        assertThat(cmd.price()).isEqualTo(2000000L);

        verify(productService).getProduct(productId);
        verifyNoMoreInteractions(productService);
    }

    @Test
    @DisplayName("PATCH /api/products/{productId} - validation 실패면 400 + service 미호출")
    void update_validationFail_400() throws Exception {
        UUID productId = UUID.fromString("00000000-0000-0000-0000-000000000004");

        ProductRequest.Update invalid = new ProductRequest.Update("수정명", -1L); // @Positive

        mockMvc.perform(
                        patch("/api/products/{productId}", productId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalid))
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(productService);
    }

    @Test
    @DisplayName("DELETE /api/products/{productId} - service 호출 + 200")
    void delete_success() throws Exception {
        UUID productId = UUID.fromString("00000000-0000-0000-0000-000000000004");

        doNothing().when(productService).deleteProduct(productId);

        mockMvc.perform(delete("/api/products/{productId}", productId))
                .andExpect(status().isOk());

        verify(productService).deleteProduct(productId);
        verifyNoMoreInteractions(productService);
    }
}
