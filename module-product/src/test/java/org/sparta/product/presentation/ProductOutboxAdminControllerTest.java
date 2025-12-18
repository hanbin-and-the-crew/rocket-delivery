package org.sparta.product.presentation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sparta.product.application.service.ProductOutboxAdminService;
import org.sparta.product.domain.outbox.ProductOutboxEvent;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductOutboxAdminController.class)
class ProductOutboxAdminControllerTest {

    @MockBean
    private ProductOutboxAdminService productOutboxAdminService;

    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @jakarta.annotation.Resource
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/product/outbox/failed - limit 전달 시 service 호출 + 200")
    void getFailed_withLimit() throws Exception {
        when(productOutboxAdminService.getFailedEvents(10)).thenReturn(List.<ProductOutboxEvent>of());

        mockMvc.perform(get("/api/product/outbox/failed").param("limit", "10"))
                .andExpect(status().isOk());

        verify(productOutboxAdminService).getFailedEvents(10);
        verifyNoMoreInteractions(productOutboxAdminService);
    }

    @Test
    @DisplayName("GET /api/product/outbox/failed - limit 미전달(default=100) service 호출 + 200")
    void getFailed_defaultLimit() throws Exception {
        when(productOutboxAdminService.getFailedEvents(100)).thenReturn(List.<ProductOutboxEvent>of());

        mockMvc.perform(get("/api/product/outbox/failed"))
                .andExpect(status().isOk());

        verify(productOutboxAdminService).getFailedEvents(100);
        verifyNoMoreInteractions(productOutboxAdminService);
    }
}
