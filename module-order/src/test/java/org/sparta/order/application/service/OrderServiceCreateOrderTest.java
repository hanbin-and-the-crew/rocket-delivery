package org.sparta.order.application.service;

import feign.FeignException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sparta.common.event.EventPublisher;
import org.sparta.order.application.command.OrderCommand;
import org.sparta.order.domain.entity.Order;
import org.sparta.order.domain.error.OrderErrorType;
import org.sparta.order.domain.repository.OrderRepository;
import org.sparta.order.infrastructure.client.CouponClient;
import org.sparta.order.infrastructure.client.PaymentClient;
import org.sparta.order.infrastructure.client.PointClient;
import org.sparta.order.infrastructure.client.StockClient;
import org.sparta.order.infrastructure.event.publisher.OrderCreatedEvent;
import org.sparta.order.presentation.dto.response.OrderResponse;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceCreateOrderTest {

    @InjectMocks
    private OrderService orderService;

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private StockClient stockClient;
    @Mock
    private PointClient pointClient;
    @Mock
    private CouponClient couponClient;
    @Mock
    private PaymentClient paymentClient;

    @Test
    void createOrder_success_withPointAndCoupon_andPublishOrderCreatedEvent() {
        // ===== given =====
        UUID customerId = UUID.randomUUID();
        UUID supplierCompanyId = UUID.randomUUID();
        UUID supplierHubId = UUID.randomUUID();
        UUID receiptCompanyId = UUID.randomUUID();
        UUID receiptHubId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        int quantity = 3;
        int productPrice = 10_000; // 1만원
        long totalPrice = (long) quantity * productPrice; // 30,000
        long requestPoint = 2_000L;
        long couponDiscount = 5_000L;
        long expectedAmountPayable = totalPrice - requestPoint - couponDiscount; // 23,000

        OrderCommand.Create command = new OrderCommand.Create(
                supplierCompanyId,
                supplierHubId,
                receiptCompanyId,
                receiptHubId,
                productId,
                quantity,
                productPrice,
                "서울특별시 강남구 테헤란로 123",
                "홍길동",
                "010-1111-2222",
                "slack-1234",
                LocalDateTime.now().plusDays(1),   // DueAtValue 검증 통과용 (미래 시각)
                "빠른 배송 부탁드립니다.",
                (int) requestPoint,
                "CARD",
                "TOSS",
                "KRW",
                "COUPON-001"
        );

        // --- Repository.save 호출 시, JPA처럼 ID 세팅해주는 스텁 ---
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> {
                    Order o = invocation.getArgument(0);
                    try {
                        Field idField = Order.class.getDeclaredField("id");
                        idField.setAccessible(true);
                        if (idField.get(o) == null) {
                            idField.set(o, UUID.randomUUID());
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    return o;
                });

        // --- FeignClient 스텁: 재고 예약 ---
        UUID fakeStockReservationId = UUID.randomUUID();
        when(stockClient.reserveStock(any(StockClient.StockReserveRequest.class)))
                .thenReturn(new StockClient.StockReserveResponse(
                        fakeStockReservationId,
                        UUID.randomUUID(),   // stockId
                        "dummy-reservation-key",
                        quantity,
                        "RESERVED"
                ));

        // --- FeignClient 스텁: 포인트 예약 ---
        String pointReservationId = "POINT-RES-001";
        when(pointClient.reservePoint(any(PointClient.PointReserveRequest.class)))
                .thenReturn(new PointClient.PointReserveResponse(pointReservationId));

        // --- FeignClient 스텁: 쿠폰 예약 ---
        String couponReservationId = "COUPON-RES-001";
        when(couponClient.reserveCoupon(eq("COUPON-001"), any(CouponClient.CouponReserveRequest.class)))
                .thenReturn(new CouponClient.CouponReserveResponse(
                        true,
                        couponReservationId,
                        couponDiscount,
                        "FIXED",
                        LocalDateTime.now().plusDays(1),
                        null,
                        null
                ));

        // --- FeignClient 스텁: 결제 승인 ---
        String pgToken = "pg_abc123";
        when(paymentClient.approvePayment(
                anyString(),
                any(PaymentClient.PaymentApproveRequest.class)
        )).thenReturn(new PaymentClient.PaymentApproveResponse(
                pgToken,
                "payment-key-123"
        ));

        // ===== when =====
        OrderResponse.Detail result = orderService.createOrder(customerId, command);

        // ===== then =====

        // 1) OrderRepository.save 가 한 번 호출되었는지
        verify(orderRepository, times(1)).save(any(Order.class));

        // 2) 재고 예약 FeignClient 호출 검증
        ArgumentCaptor<StockClient.StockReserveRequest> stockReqCaptor =
                ArgumentCaptor.forClass(StockClient.StockReserveRequest.class);
        verify(stockClient, times(1)).reserveStock(stockReqCaptor.capture());

        StockClient.StockReserveRequest stockReq = stockReqCaptor.getValue();
        assertEquals(productId, stockReq.productId());
        assertEquals(quantity, stockReq.quantity());
        // reservationKey는 orderId.toString() 이 들어가야 함 (null 여부만 우선 확인)
        assertNotNull(stockReq.reservationKey());

        // 3) 포인트 예약 FeignClient 호출 검증
        ArgumentCaptor<PointClient.PointReserveRequest> pointReqCaptor =
                ArgumentCaptor.forClass(PointClient.PointReserveRequest.class);
        verify(pointClient, times(1)).reservePoint(pointReqCaptor.capture());

        PointClient.PointReserveRequest pointReq = pointReqCaptor.getValue();
        assertEquals(String.valueOf(customerId), pointReq.userId());
        assertEquals(totalPrice, pointReq.orderAmount());
        assertEquals(requestPoint, pointReq.requestPoint());

        // 4) 쿠폰 예약 FeignClient 호출 검증
        ArgumentCaptor<CouponClient.CouponReserveRequest> couponReqCaptor =
                ArgumentCaptor.forClass(CouponClient.CouponReserveRequest.class);
        // couponId 는 "COUPON-001" 이어야 함
        verify(couponClient, times(1))
                .reserveCoupon(eq("COUPON-001"), couponReqCaptor.capture());

        CouponClient.CouponReserveRequest couponReq = couponReqCaptor.getValue();
        assertEquals(totalPrice, couponReq.orderAmount());

        // 5) 결제 승인 FeignClient 호출 검증
        ArgumentCaptor<PaymentClient.PaymentApproveRequest> payReqCaptor =
                ArgumentCaptor.forClass(PaymentClient.PaymentApproveRequest.class);
        verify(paymentClient, times(1))
                .approvePayment(anyString(), payReqCaptor.capture());

        PaymentClient.PaymentApproveRequest payReq = payReqCaptor.getValue();
        assertEquals(expectedAmountPayable, payReq.amountPayable());
        assertEquals(command.methodType(), payReq.methodType());
        assertEquals(command.pgProvider(), payReq.pgProvider());
        assertEquals(command.currency(), payReq.currency());

        // 6) OrderCreatedEvent 발행 검증
        ArgumentCaptor<OrderCreatedEvent> eventCaptor =
                ArgumentCaptor.forClass(OrderCreatedEvent.class);
        verify(eventPublisher, times(1)).publishExternal(eventCaptor.capture());

        OrderCreatedEvent event = eventCaptor.getValue();
        assertNotNull(event.eventId());
        assertEquals(result.orderId(), event.orderId());
        assertEquals(totalPrice, event.orderAmount());
        assertEquals(requestPoint, event.requestPoint());
        assertEquals(couponDiscount, event.discountAmount());
        assertEquals(expectedAmountPayable, event.amountPayable());
        assertEquals(pointReservationId, event.pointReservationId());
        assertEquals(couponReservationId, event.couponReservationId());
        assertEquals(pgToken, event.pgToken());

        // 7) 응답 객체 기본 검증 (서비스 리턴 값)
        assertNotNull(result.orderId());
        assertEquals(customerId, result.customerId());
        assertEquals(productId, result.productId());
        assertEquals(quantity, result.quantity());
        assertEquals(totalPrice, result.totalPrice());
    }
}
