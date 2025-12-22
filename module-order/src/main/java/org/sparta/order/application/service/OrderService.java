package org.sparta.order.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.common.error.BusinessException;
import org.sparta.common.event.EventPublisher;
import org.sparta.common.event.order.OrderApprovedEvent;
import org.sparta.common.event.order.OrderCancelledEvent;
import org.sparta.common.event.order.OrderCreatedEvent;
import org.sparta.common.event.order.OrderDeletedEvent;
import org.sparta.order.application.command.OrderCommand;
import org.sparta.order.application.error.OrderApplicationErrorType;
import org.sparta.order.application.error.ServiceUnavailableException;
import org.sparta.order.domain.entity.OrderOutboxEvent;
import org.sparta.order.domain.repository.OrderOutboxEventRepository;
import org.sparta.order.application.dto.*;
import org.sparta.order.application.service.reservation.*;
import org.sparta.order.presentation.dto.response.OrderResponse;
import org.sparta.order.domain.entity.Order;
import org.sparta.order.domain.enumeration.CanceledReasonCode;
import org.sparta.order.domain.error.OrderErrorType;
import org.sparta.order.domain.circuitbreaker.CircuitBreaker;
import org.sparta.order.domain.repository.OrderRepository;
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

    private final StockReservationService stockReservationService;
    private final PointReservationService pointReservationService;
    private final CouponReservationService couponReservationService;
    private final PaymentApprovalService paymentApprovalService;
    private final CircuitBreaker circuitBreaker;

    // ===== 페이지 사이즈 / 정렬 기본값  =====
    private static final List<Integer> ALLOWED_PAGE_SIZES = List.of(10, 30, 50);
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final String DEFAULT_SORT_PROPERTY = "createdAt";
    private static final List<String> CRITICAL_SERVICES = List.of(
        "stock-service",
        "point-service",
        "coupon-service",
        "payment-service"
    );

    // 단건 조회 (내부)
    private Order findOrderOrThrow(UUID orderId) {
        return orderRepository.findByIdAndDeletedAtIsNull(orderId)
                .orElseThrow(() -> new BusinessException(OrderApplicationErrorType.ORDER_NOT_FOUND));
    }

    /**
     * 허용 사이즈 : 10(기본값), 30, 50
     * - sort 미지정 시: createdAt DESC 기본
     *
     */
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

    /**
     * 주문 진입 시점에서 Circuit Breaker 상태를 점검하여
     * 지속 장애가 감지된 외부 서비스에 대한 호출을 차단한다.
     */
    private void validateCircuitBreakerState() {
        for (String service : CRITICAL_SERVICES) {
            if (circuitBreaker.isOpen(service)) {
                log.warn("[Fail Fast] Circuit Breaker OPEN - service={}, 요청 차단", service);
                throw new ServiceUnavailableException(
                    "현재 주문 처리가 불가능합니다. 잠시 후 다시 시도해 주세요."
                );
            }
        }
    }

    // ===== 주문 생성 =====

    /**
     * 멱등성이 적용된 주문 생성
     * - 동일한 idempotencyKey로 재요청 시 기존 결과 반환
     */
    public OrderResponse.Detail createOrder(UUID customerId, OrderCommand.Create request,
                                            String idempotencyKey) {
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
     * - Circuit Breaker는 각 ReservationService에서 자동 처리됨
     */
    public OrderResponse.Detail createOrder(UUID customerId, OrderCommand.Create request) {
        validateCircuitBreakerState();
        // ===================== 1. Order 엔티티 생성 =====================
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
        log.info("[Order] 저장 완료 - orderId={}", orderId);

        Long requestPoint = request.requestPoint() != null ? request.requestPoint() : 0L;

        try {
            // ===================== 2. 재고 예약 (Circuit Breaker 적용) =====================
            StockReservationResult stock = stockReservationService.reserve(
                savedOrder.getProductId(),
                orderId.toString(),
                savedOrder.getQuantity().getValue()
            );

            // ===================== 3. 포인트 예약 (Circuit Breaker 적용, 선택적) =====================
            PointReservationResult point = pointReservationService.reserve(
                savedOrder.getCustomerId(),
                orderId,
                savedOrder.getTotalPrice().getAmount(),
                requestPoint
            );

            Long usedPointAmount = (point != null) ? point.usedAmount() : 0L;
            String pointReservationId = (point != null) ? point.reservationId() : null;

            // ===================== 4. 쿠폰 예약 (Circuit Breaker 적용, 선택적) =====================
            CouponReservationResult coupon = couponReservationService.reserve(
                request.couponId(),
                savedOrder.getCustomerId(),
                orderId,
                savedOrder.getTotalPrice().getAmount()
            );

            Long usedCouponAmount = (coupon != null) ? coupon.discountAmount() : 0L;

            // ===================== 5. 결제 금액 계산 =====================
            long amountPayable = savedOrder.getTotalPrice().getAmount() - usedPointAmount - usedCouponAmount;
            if (amountPayable < 0) {
                log.warn("[결제] 결제액이 음수 - 0으로 조정: {}", amountPayable);
                amountPayable = 0;
            }

            // ===================== 6. 결제 승인 (Circuit Breaker 적용) =====================
            PaymentApprovalResult payment = paymentApprovalService.approve(
                orderId,
                customerId,
                amountPayable,
                request.methodType(),
                request.pgProvider(),
                request.currency()
            );

            // ===================== 7. OrderCreatedEvent 발행 =====================
            log.info("[이벤트] 발행 준비 - orderId={}", orderId);

            OrderCreatedEvent event = OrderCreatedEvent.of(
                    orderId,
                    savedOrder.getTotalPrice().getAmount(), // 주문 총액
                    usedCouponAmount,   // 쿠폰 차감액
                    requestPoint,   // 사용할 포인트
                    amountPayable,  // 실제 결제 액수
                    request.methodType(),
                    request.pgProvider(),
                    request.currency(),
                    request.couponId(),    // 쿠폰 예약 Id
                    pointReservationId != null ? UUID.fromString(pointReservationId) : null, // 포인트 예약 Id
                    payment.paymentKey()
            );


            // ===================== 8. Outbox 패턴 적용 =====================
            String payload;
            try {
                payload = objectMapper.writeValueAsString(event);
            } catch (JsonProcessingException e) {
                log.error("[Outbox] 이벤트 직렬화 실패", e);
                throw new RuntimeException("OrderCreatedEvent 직렬화 실패", e);
            }

            OrderOutboxEvent outbox = OrderOutboxEvent.ready(
                    "ORDER",
                    orderId,
                    "OrderCreatedEvent",
                    payload
            );

            outboxRepository.save(outbox);

//        eventPublisher.publishExternal(event);

            log.info("[Outbox] 이벤트 저장 완료 - outboxId={}, orderId={}, status=READY",
                    outbox.getId(), orderId);

            return OrderResponse.Detail.from(savedOrder);

        } catch (Exception e) {
            // ===================== Saga 보상 트랜잭션 =====================
            log.error("[Saga] 주문 생성 실패 - 보상 트랜잭션 시작. orderId={}", orderId, e);
            compensate(orderId, savedOrder.getProductId(), savedOrder.getQuantity().getValue(), e);
            throw e;
        }
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
        OrderApprovedEvent event = OrderApprovedEvent.of(
                order.getId(),                 // orderId
                order.getCustomerId(),         // customerId
                order.getSupplierCompanyId(),  // supplierCompanyId
                order.getSupplierHubId(),      // supplierHubId
                order.getReceiveCompanyId(),   // receiveCompanyId
                order.getReceiveHubId(),       // receiveHubId
                order.getAddress(),            // address
                order.getUserName(),           // receiverName
                order.getSlackId(),            // receiverSlackId
                order.getUserPhoneNumber(),    // receiverPhone
                order.getDueAt().getTime(),    // dueAt (LocalDateTime)
                order.getRequestMemo()         // requestMemo
        );
        eventPublisher.publishExternal(event);
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
        OrderCancelledEvent event = OrderCancelledEvent.of(
                order.getId(),
                order.getProductId(),
                order.getQuantity().getValue()
        );
        eventPublisher.publishExternal(event);

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
        OrderDeletedEvent event = OrderDeletedEvent.of(
                order.getId()
        );
        eventPublisher.publishExternal(event);

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


    // ===== Saga 보상 트랜잭션 =====

    /**
     * 주문 생성 실패 시 OrderCancelledEvent를 발행하여 각 서비스가 롤백하도록 함
     * Point/Coupon/Product 모듈이 구독하고 orderId 기준으로 예약 취소 처리
     */
    private void compensate(
            UUID orderId,
            UUID productId,
            Integer quantity,
            Exception cause
    ) {
        log.error("[Saga] 보상 트랜잭션 시작 - orderId={}, reason={}", orderId, cause.getMessage());

        try {
            OrderCancelledEvent event = OrderCancelledEvent.of(
                    orderId,
                    productId,
                    quantity
            );
            eventPublisher.publishExternal(event);
            log.info("[Saga] 보상 이벤트(OrderCancelledEvent) 발행 완료 - orderId={}", orderId);
        } catch (Exception e) {
            log.error("[Saga] 보상 이벤트 발행 실패 - 수동 개입 필요. orderId={}", orderId, e);
        }
    }
}
