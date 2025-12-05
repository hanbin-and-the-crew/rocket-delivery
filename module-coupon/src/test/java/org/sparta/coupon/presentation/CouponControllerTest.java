package org.sparta.coupon.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sparta.common.error.BusinessException;
import org.sparta.coupon.application.dto.CouponServiceResult;
import org.sparta.coupon.application.service.CouponService;
import org.sparta.coupon.domain.enums.CouponStatus;
import org.sparta.coupon.domain.enums.DiscountType;
import org.sparta.coupon.domain.error.CouponErrorType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CouponController.class)
@DisplayName("CouponController 테스트")
class CouponControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CouponService couponService;

    @Test
    @DisplayName("유효한 쿠폰 예약 요청을 보내면 성공 응답을 반환한다")
    void reserveCoupon_WithValidRequest_ReturnsSuccessResponse() throws Exception {
        
        UUID couponId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID reservationId = UUID.randomUUID();

        CouponRequest.Reserve request = new CouponRequest.Reserve(
                userId,
                orderId,
                50000L
        );

        CouponServiceResult.Reserve result = new CouponServiceResult.Reserve(
                reservationId,
                5000L,
                DiscountType.FIXED,
                LocalDateTime.now().plusMinutes(5)
        );

        given(couponService.reserveCoupon(eq(request), eq(couponId)))
                .willReturn(result);

        
        mockMvc.perform(post("/api/coupons/{couponId}/reserve", couponId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valid").value(true))
                .andExpect(jsonPath("$.data.reservationId").value(reservationId.toString()))
                .andExpect(jsonPath("$.data.discountAmount").value(5000L))
                .andExpect(jsonPath("$.data.discountType").value("FIXED"))
                .andExpect(jsonPath("$.data.expiresAt").exists());

        verify(couponService).reserveCoupon(eq(request), eq(couponId));
    }

    @Test
    @DisplayName("만료된 쿠폰 예약 요청을 보내면 검증 실패 응답을 반환한다")
    void reserveCoupon_WithExpiredCoupon_ReturnsFailureResponse() throws Exception {
        
        UUID couponId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        CouponRequest.Reserve request = new CouponRequest.Reserve(
                userId,
                orderId,
                50000L
        );

        given(couponService.reserveCoupon(eq(request), eq(couponId)))
                .willThrow(new BusinessException(CouponErrorType.COUPON_EXPIRED));

        
        mockMvc.perform(post("/api/coupons/{couponId}/reserve", couponId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valid").value(false))
                .andExpect(jsonPath("$.data.errorCode").value("coupon:coupon_expired"))
                .andExpect(jsonPath("$.data.message").value("만료된 쿠폰입니다"));

        verify(couponService).reserveCoupon(eq(request), eq(couponId));
    }

    @Test
    @DisplayName("최소 주문 금액 미달 시 검증 실패 응답을 반환한다")
    void reserveCoupon_WithInsufficientOrderAmount_ReturnsFailureResponse() throws Exception {
        
        UUID couponId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        CouponRequest.Reserve request = new CouponRequest.Reserve(
                userId,
                orderId,
                20000L
        );

        given(couponService.reserveCoupon(eq(request), eq(couponId)))
                .willThrow(new BusinessException(CouponErrorType.INSUFFICIENT_ORDER_AMOUNT));

        
        mockMvc.perform(post("/api/coupons/{couponId}/reserve", couponId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valid").value(false))
                .andExpect(jsonPath("$.data.errorCode").value("coupon:insufficient_order_amount"))
                .andExpect(jsonPath("$.data.message").value("최소 주문 금액을 만족하지 않습니다"));

        verify(couponService).reserveCoupon(eq(request), eq(couponId));
    }

    @Test
    @DisplayName("필수 필드가 없는 예약 요청을 보내면 400 에러를 반환한다")
    void reserveCoupon_WithMissingFields_ReturnsBadRequest() throws Exception {
        
        UUID couponId = UUID.randomUUID();
        String invalidRequest = "{}";

        
        mockMvc.perform(post("/api/coupons/{couponId}/reserve", couponId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest());

        verify(couponService, never()).reserveCoupon(any(CouponRequest.Reserve.class), any(UUID.class));
    }

    @Test
    @DisplayName("유효한 쿠폰 사용 확정 요청을 보내면 성공 응답을 반환한다")
    void confirmCoupon_WithValidRequest_ReturnsSuccessResponse() throws Exception {
        
        UUID reservationId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        CouponRequest.Confirm request = new CouponRequest.Confirm(
                reservationId,
                orderId
        );

        doNothing().when(couponService).confirmCoupon(reservationId, orderId);

        
        mockMvc.perform(post("/api/coupons/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.success").value(true))
                .andExpect(jsonPath("$.data.usedAt").exists());

        verify(couponService).confirmCoupon(reservationId, orderId);
    }

    @Test
    @DisplayName("존재하지 않는 예약을 확정하려고 하면 예외가 발생한다")
    void confirmCoupon_WithNonExistentReservation_ThrowsException() throws Exception {
        
        UUID reservationId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        CouponRequest.Confirm request = new CouponRequest.Confirm(
                reservationId,
                orderId
        );

        doThrow(new BusinessException(CouponErrorType.RESERVATION_NOT_FOUND))
                .when(couponService).confirmCoupon(reservationId, orderId);

        
        mockMvc.perform(post("/api/coupons/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());

        verify(couponService).confirmCoupon(reservationId, orderId);
    }

    @Test
    @DisplayName("유효한 쿠폰 예약 취소 요청을 보내면 성공 응답을 반환한다")
    void cancelReservation_WithValidRequest_ReturnsSuccessResponse() throws Exception {
        
        UUID reservationId = UUID.randomUUID();

        CouponRequest.Cancel request = new CouponRequest.Cancel(
                reservationId,
                "ORDER_FAILED"
        );

        doNothing().when(couponService).cancelReservation(reservationId);

        
        mockMvc.perform(post("/api/coupons/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.success").value(true))
                .andExpect(jsonPath("$.data.status").value(CouponStatus.AVAILABLE.name()));

        verify(couponService).cancelReservation(reservationId);
    }

    @Test
    @DisplayName("존재하지 않는 예약을 취소하려고 하면 예외가 발생한다")
    void cancelReservation_WithNonExistentReservation_ThrowsException() throws Exception {
        
        UUID reservationId = UUID.randomUUID();

        CouponRequest.Cancel request = new CouponRequest.Cancel(
                reservationId,
                "ORDER_FAILED"
        );

        doThrow(new BusinessException(CouponErrorType.RESERVATION_NOT_FOUND))
                .when(couponService).cancelReservation(reservationId);

        
        mockMvc.perform(post("/api/coupons/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());

        verify(couponService).cancelReservation(reservationId);
    }
}
