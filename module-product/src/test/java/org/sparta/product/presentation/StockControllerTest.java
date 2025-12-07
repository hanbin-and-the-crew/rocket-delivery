package org.sparta.product.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.sparta.product.application.service.StockService;
import org.sparta.product.domain.entity.StockReservation;
import org.sparta.product.presentation.dto.stock.StockRequest;
import org.sparta.product.presentation.dto.stock.StockResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
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
 * StockController WebMvc 슬라이스 테스트
 * - 재고 예약/확정/취소 API 의 HTTP 레이어 검증
 */
@WebMvcTest(StockController.class)
@ActiveProfiles("test")
class StockControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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

        // 엔티티 팩토리 메서드 사용 (id 는 null 이어도 상관 없음)
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
                // ApiResponse 구조는 몰라도, 예약 키/수량이 포함되는지만 확인
                .andExpect(content().string(containsString(reservationKey)))
                .andExpect(content().string(containsString(String.valueOf(quantity))));

        // then - 서비스 호출 인자 검증
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
