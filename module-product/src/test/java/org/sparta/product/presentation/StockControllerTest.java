package org.sparta.product.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sparta.product.application.service.StockService;
import org.sparta.product.domain.entity.StockReservation;
import org.sparta.product.presentation.dto.stock.StockRequest;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StockController.class)
class StockControllerTest {

    @MockBean
    private StockService stockService;

    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @jakarta.annotation.Resource
    private MockMvc mockMvc;

    @jakarta.annotation.Resource
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/product/stocks/reserve - 유효 요청이면 reserveStock 호출 + 200")
    void reserve_success() throws Exception {
        UUID productId = UUID.fromString("00000000-0000-0000-0000-000000000010");

        StockRequest.Reserve request = new StockRequest.Reserve(
                productId,
                "order-20251217-0001",
                3
        );

        // 반환용 StockReservation mock을 '응답 변환에 필요한 값'까지 스텁해줘야 500이 안 남.
        StockReservation reservation = mock(StockReservation.class);

        UUID reservationId = UUID.fromString("00000000-0000-0000-0000-000000000111");
        UUID stockId = UUID.fromString("00000000-0000-0000-0000-000000000222");

        when(reservation.getId()).thenReturn(reservationId);
        when(reservation.getStockId()).thenReturn(stockId);
        when(reservation.getReservationKey()).thenReturn("order-20251217-0001");
        when(reservation.getReservedQuantity()).thenReturn(3);
        when(reservation.getStatus()).thenReturn(org.sparta.product.domain.enums.StockReservationStatus.RESERVED);

        when(stockService.reserveStock(eq(productId), eq("order-20251217-0001"), eq(3)))
                .thenReturn(reservation);

        mockMvc.perform(
                        post("/api/product/stocks/reserve")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk());

        verify(stockService).reserveStock(productId, "order-20251217-0001", 3);
        verifyNoMoreInteractions(stockService);
    }


    @Test
    @DisplayName("POST /api/product/stocks/reserve - validation 실패면 400 + service 미호출")
    void reserve_validationFail_400() throws Exception {
        UUID productId = UUID.fromString("00000000-0000-0000-0000-000000000010");

        StockRequest.Reserve invalid = new StockRequest.Reserve(
                productId,
                " ", // @NotBlank
                0    // @Positive
        );

        mockMvc.perform(
                        post("/api/product/stocks/reserve")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalid))
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(stockService);
    }

    @Test
    @DisplayName("POST /api/product/stocks/confirm - 유효 요청이면 confirmReservation 호출 + 200")
    void confirm_success() throws Exception {
        StockRequest.Confirm request = new StockRequest.Confirm("order-20251217-0001");

        doNothing().when(stockService).confirmReservation("order-20251217-0001");

        mockMvc.perform(
                        post("/api/product/stocks/confirm")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk());

        verify(stockService).confirmReservation("order-20251217-0001");
        verifyNoMoreInteractions(stockService);
    }

    @Test
    @DisplayName("POST /api/product/stocks/cancel - 유효 요청이면 cancelReservation 호출 + 200")
    void cancel_success() throws Exception {
        StockRequest.Cancel request = new StockRequest.Cancel("order-20251217-0001");

        doNothing().when(stockService).cancelReservation("order-20251217-0001");

        mockMvc.perform(
                        post("/api/product/stocks/cancel")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk());

        verify(stockService).cancelReservation("order-20251217-0001");
        verifyNoMoreInteractions(stockService);
    }

    @Test
    @DisplayName("POST /api/product/stocks/confirm - validation 실패면 400 + service 미호출")
    void confirm_validationFail_400() throws Exception {
        StockRequest.Confirm invalid = new StockRequest.Confirm(" "); // @NotBlank

        mockMvc.perform(
                        post("/api/product/stocks/confirm")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalid))
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(stockService);
    }

    @Test
    @DisplayName("POST /api/product/stocks/cancel - validation 실패면 400 + service 미호출")
    void cancel_validationFail_400() throws Exception {
        StockRequest.Cancel invalid = new StockRequest.Cancel(""); // @NotBlank

        mockMvc.perform(
                        post("/api/product/stocks/cancel")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalid))
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(stockService);
    }
}
