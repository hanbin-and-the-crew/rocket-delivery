package org.sparta.order.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.common.api.ApiResponse;
import org.sparta.common.error.BusinessException;
import org.sparta.common.event.EventPublisher;
import org.sparta.order.application.command.OrderCommand;
import org.sparta.order.application.error.OrderApplicationErrorType;
import org.sparta.order.domain.entity.OrderOutboxEvent;
import org.sparta.order.domain.repository.OrderOutboxEventRepository;
import org.sparta.order.infrastructure.client.CouponClient;
import org.sparta.order.infrastructure.client.PaymentClient;
import org.sparta.order.infrastructure.client.PointClient;
import org.sparta.order.infrastructure.client.StockClient;
import org.sparta.order.infrastructure.event.publisher.OrderDeletedEvent;
import org.sparta.order.presentation.dto.response.OrderResponse;
import org.sparta.order.domain.entity.Order;
import org.sparta.order.domain.enumeration.CanceledReasonCode;
import org.sparta.order.domain.error.OrderErrorType;
import org.sparta.order.domain.repository.OrderRepository;
import org.sparta.order.infrastructure.event.publisher.OrderApprovedEvent;
import org.sparta.order.infrastructure.event.publisher.OrderCancelledEvent;
import org.sparta.order.infrastructure.event.publisher.OrderCreatedEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderOutboxEventRepository outboxRepository;
    private final EventPublisher eventPublisher; // Kafka + Spring Event 래퍼 (이미 common 모듈에 있음)
    private final ObjectMapper objectMapper;

    private final IdempotencyService idempotencyService;

    private final StockClient stockClient;
    private final PointClient pointClient;
    private final CouponClient couponClient;
    private final PaymentClient paymentClient;

    // ===== 페이지 사이즈 / 정렬 기본값  =====
    private static final List<Integer> ALLOWED_PAGE_SIZES = List.of(10, 30, 50);
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final String DEFAULT_SORT_PROPERTY = "createdAt";

    // 단건 조회 (내부)
    private Order findOrderOrThrow(UUID orderId) {
        return orderRepository.findByIdAndDeletedAtIsNull(orderId)
                .orElseThrow(() -> new BusinessException(OrderApplicationErrorType.ORDER_NOT_FOUND));
    }

    /**
     * 허용 사이즈 : 10(기본값), 30, 50
     * - sort 미지정 시: createdAt DESC 기본
     * */
    private Pageable normalizePageable(Pageable pageable) {
        int page = pageable.getPageNumber();
        int size = pageable.getPageSize();

        if (!ALLOWED_PAGE_SIZES.contains(size)) {
            size = DEFAULT_PAGE_SIZE;
        }

        Sort sort = pageable.getSort();
        if (sort == null || sort.isUnsorted()) {
            sort = Sort.by(Sort.Direction.DESC, DEFAULT_SORT_PROPERTY);
        }

        return PageRequest.of(page, size, sort);
    }

    // ===== 주문 생성 =====

    /**
     * 멱등성이 적용된 주문 생성
     * - 동일한 idempotencyKey로 재요청 시 기존 결과 반환
     */
    public OrderResponse.Detail createOrder(UUID customerId, OrderCommand.Create request,
                                            String idempotencyKey) throws JsonProcessingException {
        // 1. 이미 처리된 요청인지 확인
        Optional<OrderResponse.Detail> existingResponse =
                idempotencyService.findExistingResponse(idempotencyKey);

        if (existingResponse.isPresent()) {
            log.info("Idempotent request detected - returning cached response. key={}", idempotencyKey);
            return existingResponse.get();
        }

        // 2. Lock 획득 시도 (동시 요청 방지)
        if (!idempotencyService.tryAcquireLock(idempotencyKey)) {
            log.warn("Concurrent request detected for idempotencyKey={}", idempotencyKey);
            // 잠시 대기 후 재조회하거나 에러 반환
            throw new BusinessException(OrderErrorType.REQUEST_IN_PROGRESS);
        }

        try {
            // 3. 기존 주문 생성 로직 실행
            OrderResponse.Detail response = createOrder(customerId, request);

            // 4. 멱등성 레코드 저장
            idempotencyService.saveIdempotencyRecord(
                    idempotencyKey,
                    response.orderId().toString(),
                    response,
                    200
            );

            return response;
        } catch (Exception e) {
            // 실패 시 placeholder 삭제 (재시도 가능하도록)
            idempotencyService.deleteRecord(idempotencyKey);
            throw e;
        }
    }

    /**
     * 주문 생성
     * - 상태: CREATED
     */
    public OrderResponse.Detail createOrder(UUID customerId, OrderCommand.Create request) {
        // 엔티티 생성
        Order order = Order.create(
                customerId,
                request.supplierCompanyId(),
                request.supplierHubId(),
                request.receiptCompanyId(),
                request.receiptHubId(),
                request.productId(),
                request.productPrice().longValue(),
                request.quantity(),
                request.dueAt(),
                request.address(),
                request.requestMemo(),
                request.userName(),
                request.userPhoneNumber(),
                request.slackId()
        );

        Order savedOrder = orderRepository.save(order);
        UUID orderId = savedOrder.getId();
        log.info("Order 저장 완료 - orderId: {}", orderId);

        long requestPoint = request.requestPoint() != null ? request.requestPoint() : 0L;

        // ===================== 2. 재고 예약 =====================
        ApiResponse<StockClient.StockReserveResponse> stockResResponse;
        try {
            stockResResponse = stockClient.reserveStock(new StockClient.StockReserveRequest(
                    savedOrder.getProductId(),
                    orderId.toString(),
                    savedOrder.getQuantity().getValue()
            ));
        } catch (FeignException e) {
            log.error("재고 예약 실패 - productId={}, quantity={}", savedOrder.getProductId(), savedOrder.getQuantity(), e);
            throw new BusinessException(OrderErrorType.STOCK_RESERVATION_FAILED);
        }

        // 1) meta.result 가 SUCCESS 인지 먼저 확인
        if (stockResResponse.meta().result() != ApiResponse.Metadata.Result.SUCCESS) {
            log.error("재고 예약 API 응답 실패 - result={}, errorCode={}",
                    stockResResponse.meta().result(),
                    stockResResponse.meta().errorCode()
            );
            throw new BusinessException(OrderErrorType.STOCK_RESERVATION_FAILED);
        }

        // 2) data 꺼내서  실제 예약 상태 확인
        StockClient.StockReserveResponse stockRes = stockResResponse.data();
        if (stockRes == null || !"RESERVED".equalsIgnoreCase(stockRes.status())) {
            log.error("재고 예약 상태 비정상 - status={}", stockRes != null ? stockRes.status() : null);
            throw new BusinessException(OrderErrorType.STOCK_RESERVATION_FAILED);
        }

        // ===================== 3. 포인트 예약 =====================
        PointClient.PointReserveResponse pointRes = null;
        if (requestPoint > 0) {
            try {
                pointRes = pointClient.reservePoint(new PointClient.PointReserveRequest(
                        savedOrder.getCustomerId().toString(),
                        orderId.toString(),
                        savedOrder.getTotalPrice().getAmount(),
                        requestPoint
                ));
            } catch (FeignException e) {
                log.error("포인트 예약 실패 - customerId={}, usedPoint={}", savedOrder.getCustomerId(), requestPoint, e);
                throw new BusinessException(OrderErrorType.POINT_RESERVATION_FAILED);
            }
        }

        String pointReservationId = pointRes != null ? pointRes.reservationId() : null;

        // ===================== 4. 쿠폰 예약 =====================
        CouponClient.CouponReserveResponse couponRes = null;
        Long usedCouponAmount = 0L;
        String couponReservationId = null;

        if (request.couponId() != null) {
            try {
                couponRes = couponClient.reserveCoupon(
                        request.couponId(),
                        new CouponClient.CouponReserveRequest(
                                savedOrder.getCustomerId().toString(),
                                orderId.toString(),
                                savedOrder.getTotalPrice().getAmount()
                        )
                );
            } catch (FeignException e) {
                log.error("쿠폰 예약 실패 - couponId={}", request.couponId(), e);
                throw new BusinessException(OrderErrorType.COUPON_RESERVATION_FAILED);
            }

            if (!couponRes.valid()) {
                log.warn("쿠폰 검증 실패 - couponId={}, errorCode={}",
                        request.couponId(), couponRes.errorCode());
                throw new BusinessException(OrderErrorType.COUPON_VALIDATION_FAILED);
            }

            usedCouponAmount = couponRes.discountAmount();
            couponReservationId = couponRes.reservationId();
        }

        // ===================== 5. 결제 승인 =====================
        long amountPayable = savedOrder.getTotalPrice().getAmount() - requestPoint - usedCouponAmount;
        if (amountPayable < 0) {
            amountPayable = 0;
        }

        PaymentClient.PaymentApproveResponse paymentRes;
        try {
            paymentRes = paymentClient.approvePayment(
                    orderId.toString(),
                    new PaymentClient.PaymentApproveRequest(
//                            orderId.toString(),
                            amountPayable,
                            request.methodType(),
                            request.pgProvider(),
                            request.currency()
                    )
            );
        } catch (FeignException e) {
            log.error("결제 승인 실패 - orderId={}, amountPayable={}", orderId, amountPayable, e);
            throw new BusinessException(OrderErrorType.PAYMENT_APPROVE_FAILED);
        }

        String pgToken = paymentRes.pgToken();

        // ===================== 7. OrderCreatedEvent 발행 =====================
        OrderCreatedEvent event = OrderCreatedEvent.of(
                orderId,
                savedOrder.getTotalPrice().getAmount(), // 주문 총액
                requestPoint,   // 사용할 포인트
                usedCouponAmount,   // 쿠폰 차감액
                amountPayable,  // 실제 결제 액수 (주문 총액 - 사용 포인트 - 쿠폰 차감액)
                pointReservationId, // 포인트 예약 Id
                couponReservationId,    // 쿠폰 예약 Id
                pgToken
        );


        // ===================== 8. Outbox 패턴 적용 =====================
        String payload = null;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        OrderOutboxEvent outbox = OrderOutboxEvent.ready(
                "ORDER",
                orderId,
                "OrderCreatedEvent",
                payload
        );

        outboxRepository.save(outbox);

        // eventPublisher.publishExternal(event);

        return OrderResponse.Detail.from(savedOrder);
    }


    // ===== 주문 상태 변경 처리 =====
    // 결제 성공 -> 재고 감소 -> 감소 성공 -> approveOrder()
    /**
     * stock에서 "재고 감소 완료" 이벤트가 온 뒤 호출된다고 가정.
     * - CREATED → APPROVED
     */
    public void approveOrder(UUID orderId, UUID paymentId) {
        Order order = findOrderOrThrow(orderId);

        // 주문 상태 변경: PENDING → APPROVED
        order.approve(paymentId);

        // OrderApprovedEvent 발행 (배송/Slack 모듈 사용)
        // TODO: delivery에서 필요한 정보 확인 후 추가
        eventPublisher.publishExternal(OrderApprovedEvent.of(order));
    }

    // 주문 취소 처리
    public OrderResponse.Update cancelOrder(OrderCommand.Cancel request) {
        Order order = findOrderOrThrow(request.orderId());

        // String → Enum 변환
        CanceledReasonCode reasonCode;
        try {
            reasonCode = CanceledReasonCode.valueOf(request.reasonCode());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(OrderErrorType.CANCELED_REASON_CODE_REQUIRED);
        }

        // 주문 취소 처리
        order.cancel(reasonCode, request.reasonMemo());

        // 재고/예약 취소, 결제 취소를 위한 이벤트 발행
        eventPublisher.publishExternal(OrderCancelledEvent.of(order));

        return OrderResponse.Update.of(order, "주문이 취소되었습니다.");
    }

    // 배송 시작/출고 처리 (API)
    public OrderResponse.Update shipOrder(OrderCommand.ShipOrder request) {
        Order order = findOrderOrThrow(request.orderId());
        order.markShipped();
        return OrderResponse.Update.of(order, "주문이 출고(배송 시작) 처리되었습니다.");
    }
    // 배송 시작/출고 처리 (내부) _ DeliveryStartedEvent 수신 후 동작
    public OrderResponse.Update shippedOrder(UUID orderId) {
        Order order = findOrderOrThrow(orderId);
        order.markShipped();
        return OrderResponse.Update.of(order, "주문이 출고(배송 시작) 처리되었습니다.");
    }

    // 배송 완료 처리 (API)
    public OrderResponse.Update deliverOrder(OrderCommand.DeliverOrder request) {
        Order order = findOrderOrThrow(request.orderId());
        order.markDelivered();
        return OrderResponse.Update.of(order, "주문이 배송 완료 처리되었습니다.");
    }

    // 배송 완료 처리 (내부)
    public OrderResponse.Update deliveredOrder(UUID orderId) {
        Order order = findOrderOrThrow(orderId);
        order.markDelivered();
        return OrderResponse.Update.of(order, "주문이 배송 완료 처리되었습니다.");
    }

    // 삭제
    public OrderResponse.Update deleteOrder(OrderCommand.DeleteOrder request) {
        Order order = findOrderOrThrow(request.orderId());
        order.validateDeletable();
        order.markAsDeleted();

        // 주문 삭제 이벤트 발행
        eventPublisher.publishExternal(OrderDeletedEvent.of(order));

        return OrderResponse.Update.of(order, "주문이 삭제되었습니다.");
    }

    // ===== 주문 조회 =====

    // 주문 단건(상세) 조회
    @Transactional(readOnly = true)
    public OrderResponse.Detail getOrder(UUID orderId) {
        Order order = findOrderOrThrow(orderId);
        return OrderResponse.Detail.from(order);
    }

    /**
     * 고객 자신의 주문 목록 조회
     * - 페이지 사이즈: 10/30/50만 허용, 그 외 값은 10으로 보정
     * - 기본 정렬: createdAt DESC (요청에 sort가 없으면)
     */
    @Transactional(readOnly = true)
    public Page<OrderResponse.Summary> getOrdersByCustomer(UUID customerId, Pageable pageable) {
        Pageable normalized = normalizePageable(pageable);
        return orderRepository.findByCustomerIdAndDeletedAtIsNull(customerId, normalized)
                .map(OrderResponse.Summary::from);
    }

    // ===== 주문 수정 메소드 =====

    public OrderResponse.Update changeDueAt(UUID orderId, OrderCommand.ChangeDueAt request) {
        Order order = findOrderOrThrow(orderId);
        order.changeDueAt(request.dueAt());
        return OrderResponse.Update.of(order, "납기일이 변경되었습니다.");
    }

    public OrderResponse.Update changeAddress(UUID orderId, OrderCommand.ChangeAddress request) {
        Order order = findOrderOrThrow(orderId);
        order.changeAddress(request.addressSnapshot());
        return OrderResponse.Update.of(order, "주소가 변경되었습니다.");
    }

    public OrderResponse.Update changeRequestMemo(UUID orderId, OrderCommand.changeRequestMemo request) {
        Order order = findOrderOrThrow(orderId);
        order.changeRequestMemo(request.requestedMemo());
        return OrderResponse.Update.of(order, "요청사항이 변경되었습니다.");
    }
}
