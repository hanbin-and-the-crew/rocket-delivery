package org.sparta.product.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.sparta.product.application.service.StockService;
import org.sparta.product.domain.entity.StockReservation;
import org.sparta.product.presentation.dto.stock.StockRequest;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * StockController 통합 + MockMvc 테스트
 * - HTTP 레이어와 StockService 간 계약을 검증
 * - StockService 는 @MockBean 으로 대체해서 DB/Outbox 로직은 타지 않음
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StockControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // 실제 빈 대신 목으로 교체 → 서비스 레이어부터 아래는 호출 안됨
    @MockBean
    private StockService stockService;

    @Test
    @DisplayName("재고 예약 API - 성공")
    void reserveStock_success() throws Exception {
        // given
        UUID productId = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();
        String reservationKey = "order-1234";
        int quantity = 3;

        StockRequest.Reserve request = new StockRequest.Reserve(
                productId,
                reservationKey,
                quantity
        );

        StockReservation reservation = StockReservation.create(
                stockId,
                reservationKey,
                quantity
        );

        given(stockService.reserveStock(eq(productId), eq(reservationKey), eq(quantity)))
                .willReturn(reservation);

        // when
        String json = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/product/stocks/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(reservationKey)))
                .andExpect(content().string(containsString(String.valueOf(quantity))));

        // then
        ArgumentCaptor<UUID> productIdCaptor = ArgumentCaptor.forClass(UUID.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> quantityCaptor = ArgumentCaptor.forClass(Integer.class);

        then(stockService).should(times(1))
                .reserveStock(productIdCaptor.capture(), keyCaptor.capture(), quantityCaptor.capture());

        assertThat(productIdCaptor.getValue()).isEqualTo(productId);
        assertThat(keyCaptor.getValue()).isEqualTo(reservationKey);
        assertThat(quantityCaptor.getValue()).isEqualTo(quantity);
    }

    @Test
    @DisplayName("재고 확정 API - 성공")
    void confirmReservation_success() throws Exception {
        // given
        String reservationKey = "order-5678";
        StockRequest.Confirm request = new StockRequest.Confirm(reservationKey);

        // when
        mockMvc.perform(post("/api/product/stocks/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // then
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        then(stockService).should(times(1))
                .confirmReservation(keyCaptor.capture());

        assertThat(keyCaptor.getValue()).isEqualTo(reservationKey);
    }

    @Test
    @DisplayName("재고 예약 취소 API - 성공")
    void cancelReservation_success() throws Exception {
        // given
        String reservationKey = "order-9999";
        StockRequest.Cancel request = new StockRequest.Cancel(reservationKey);

        // when
        mockMvc.perform(post("/api/product/stocks/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // then
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        then(stockService).should(times(1))
                .cancelReservation(keyCaptor.capture());

        assertThat(keyCaptor.getValue()).isEqualTo(reservationKey);
    }
}
