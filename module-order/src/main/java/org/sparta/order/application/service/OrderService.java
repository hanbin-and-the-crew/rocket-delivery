package org.sparta.order.application.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.common.error.BusinessException;
import org.sparta.common.event.EventPublisher;
import org.sparta.order.application.command.OrderCommand;
import org.sparta.order.application.error.OrderApplicationErrorType;
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
    private final EventPublisher eventPublisher; // Kafka + Spring Event 래퍼 (이미 common 모듈에 있음)

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
        StockClient.StockReserveResponse stockRes;
        try {
            stockRes = stockClient.reserveStock(new StockClient.StockReserveRequest(
                    savedOrder.getProductId(),
                    orderId.toString(),
                    savedOrder.getQuantity().getValue()
            ));
        } catch (FeignException e) {
            log.error("재고 예약 실패 - productId={}, quantity={}", savedOrder.getProductId(), savedOrder.getQuantity(), e);
            throw new BusinessException(OrderErrorType.STOCK_RESERVATION_FAILED);
        }

//        if (!"RESERVED".equalsIgnoreCase(stockRes.status())) {
//            log.error("재고 예약 상태 비정상 - status={}", stockRes.status());
//            throw new BusinessException(OrderErrorType.STOCK_RESERVATION_FAILED);
//        }

        // ===================== 3. 포인트 예약 =====================
        Long usedPointAmount = 0L;
        String pointReservationId = null;

        if (requestPoint > 0) {
            log.info("[포인트] 예약 시작 - customerId={}, orderId={}, requestPoint={}",
                    savedOrder.getCustomerId(), orderId, requestPoint);

            try {
                PointClient.ApiResponse<PointClient.PointResponse.PointReservationResult> apiResponse =
                        pointClient.reservePoint(
                                new PointClient.PointRequest.Reserve(
                                        savedOrder.getCustomerId(),
                                        orderId,
                                        savedOrder.getTotalPrice().getAmount(),
                                        requestPoint
                                )
                        );

                log.info("[포인트] API 응답 - result={}, errorCode={}, message={}",
                        apiResponse.result(), apiResponse.errorCode(), apiResponse.message());

                if (!apiResponse.isSuccess()) {
                    log.error("[포인트] API 호출 실패 - errorCode={}, message={}",
                            apiResponse.errorCode(), apiResponse.message());
                    throw new BusinessException(OrderErrorType.POINT_RESERVATION_FAILED);
                }

                PointClient.PointResponse.PointReservationResult pointData = apiResponse.data();

                if (pointData == null) {
                    log.error("[포인트] 응답 데이터가 null");
                    throw new BusinessException(OrderErrorType.POINT_RESERVATION_FAILED);
                }

                log.info("[포인트] 데이터 추출 - discountAmount={}, reservations size={}",
                        pointData.discountAmount(),
                        pointData.reservations() != null ? pointData.reservations().size() : 0);

                // 실제 사용된 포인트 금액
                usedPointAmount = pointData.discountAmount() != null
                        ? pointData.discountAmount()
                        : 0L;

                // 첫 번째 예약의 ID 추출 (여러 예약이 있을 경우)
                if (pointData.reservations() != null && !pointData.reservations().isEmpty()) {
                    PointClient.PointResponse.PointReservation firstReservation =
                            pointData.reservations().get(0);

                    pointReservationId = firstReservation.id() != null
                            ? firstReservation.id().toString()
                            : null;

                    log.info("[포인트] 예약 상세 - reservationId={}, pointId={}, reservedAmount={}, status={}",
                            firstReservation.id(),
                            firstReservation.pointId(),
                            firstReservation.reservedAmount(),
                            firstReservation.status());
                } else {
                    log.warn("[포인트] 예약 목록이 비어있음");
                }

                log.info("[포인트] 예약 완료 - usedPointAmount={}, pointReservationId={}",
                        usedPointAmount, pointReservationId);

                if (usedPointAmount == 0L) {
                    log.warn("[포인트] 사용된 포인트가 0원 - requestPoint={}", requestPoint);
                }

            } catch (FeignException e) {
                log.error("[포인트] Feign 호출 실패 - status={}, message={}",
                        e.status(), e.contentUTF8(), e);
                throw new BusinessException(OrderErrorType.POINT_RESERVATION_FAILED);
            }
        } else {
            log.info("[포인트] 미사용 - requestPoint={}", requestPoint);
        }

        log.info("[포인트] 최종 값 - usedPointAmount={}, pointReservationId={}",
                usedPointAmount, pointReservationId);
        // ===================== 4. 쿠폰 예약 =====================
        Long usedCouponAmount = 0L;
        UUID couponReservationId = null;

        if (request.couponId() != null) {
            log.info("쿠폰 예약 시작 - couponId={}", request.couponId());

            try {
                // ApiResponse로 받기 (reserveCoupon의 실제 반환 타입)
                CouponClient.ApiResponse<CouponClient.CouponReserveResponse.Reserve> apiResponse =
                        couponClient.reserveCoupon(
                                request.couponId(),
                                new CouponClient.CouponRequest.Reverse(
                                        savedOrder.getCustomerId(),
                                        orderId,
                                        savedOrder.getTotalPrice().getAmount()
                                )
                        );

                log.info("쿠폰 API 응답 - result={}, errorCode={}, message={}",
                        apiResponse.result(), apiResponse.errorCode(), apiResponse.message());

                //API 호출 결과 확인
                if (!"SUCCESS".equals(apiResponse.result())) {
                    log.error("쿠폰 API 호출 실패 - errorCode={}, message={}",
                            apiResponse.errorCode(), apiResponse.message());
                    throw new BusinessException(OrderErrorType.COUPON_RESERVATION_FAILED);
                }

                // data에서 실제 쿠폰 데이터 추출
                CouponClient.CouponReserveResponse.Reserve couponData = apiResponse.data();

                if (couponData == null) {
                    log.error("쿠폰 데이터가 null - couponId={}", request.couponId());
                    throw new BusinessException(OrderErrorType.COUPON_RESERVATION_FAILED);
                }

                log.info("쿠폰 데이터 추출 - valid={}, reservationId={}, discountAmount={}",
                        couponData.valid(), couponData.reservationId(), couponData.discountAmount());

                // 쿠폰 유효성 확인
                if (!couponData.valid()) {
                    log.warn("쿠폰 검증 실패 - errorCode={}, message={}",
                            couponData.errorCode(), couponData.message());
                    throw new BusinessException(OrderErrorType.COUPON_VALIDATION_FAILED);
                }

                // 할인액과 예약 ID 할당
                usedCouponAmount = couponData.discountAmount() != null
                        ? couponData.discountAmount()
                        : 0L;
                couponReservationId = couponData.reservationId();

                log.info("쿠폰 할인 적용 완료 - usedCouponAmount={}, couponReservationId={}",
                        usedCouponAmount, couponReservationId);

                // 0원 할인 경고
                if (usedCouponAmount == 0L) {
                    log.warn("쿠폰 할인액이 0원 - couponId={}, 쿠폰 정책 확인 필요",
                            request.couponId());
                }

            } catch (FeignException e) {
                log.error("쿠폰 Feign 호출 실패 - couponId={}, status={}, message={}",
                        request.couponId(), e.status(), e.contentUTF8(), e);
                throw new BusinessException(OrderErrorType.COUPON_RESERVATION_FAILED);
            }
        } else {
            log.info("쿠폰 미사용 - couponId is null");
        }

// ===================== 5. 결제 승인 =====================
        long amountPayable = savedOrder.getTotalPrice().getAmount() - requestPoint - usedCouponAmount;
        if (amountPayable < 0) {
            log.warn("[결제] 결제액이 음수 - 0으로 조정: {}", amountPayable);
            amountPayable = 0;
        }

        log.info("[결제] 결제 승인 시작 - orderId={}, amountPayable={}, customerId={}",
                orderId, amountPayable, customerId);

        String paymentKey = null;

        try {
            // pgToken 생성 (실제로는 클라이언트로부터 받아야 함)
            String pgToken = UUID.randomUUID().toString();

            log.info("[결제] 요청 생성 - orderId={}, pgToken={}, methodType={}, pgProvider={}, currency={}",
                    orderId, pgToken, request.methodType(), request.pgProvider(), request.currency());

            PaymentClient.ApiResponse<PaymentClient.PaymentResponse.Approval> apiResponse =
                    paymentClient.approve(
                            new PaymentClient.PaymentRequest.Approval(
                                    orderId,
                                    pgToken,
                                    amountPayable,
                                    request.methodType(),      // "CARD" 등
                                    request.pgProvider(),      // "TOSS" 등
                                    request.currency()         // "KRW"
                            ),
                            customerId  // X-User-Id 헤더 (필수!)
                    );

            log.info("[결제] API 응답 수신 - result={}, errorCode={}, message={}",
                    apiResponse.result(), apiResponse.errorCode(), apiResponse.message());

            if (!apiResponse.isSuccess()) {
                log.error("[결제] API 호출 실패 - errorCode={}, message={}",
                        apiResponse.errorCode(), apiResponse.message());
                throw new BusinessException(OrderErrorType.PAYMENT_APPROVE_FAILED);
            }

            PaymentClient.PaymentResponse.Approval paymentData = apiResponse.data();

            if (paymentData == null) {
                log.error("[결제] 응답 데이터가 null");
                throw new BusinessException(OrderErrorType.PAYMENT_APPROVE_FAILED);
            }

            log.info("[결제] 데이터 추출 - orderId={}, approved={}, paymentKey={}, approvedAt={}",
                    paymentData.orderId(), paymentData.approved(),
                    paymentData.paymentKey(), paymentData.approvedAt());

            if (!paymentData.approved()) {
                log.error("[결제] 결제 승인 거부 - failureCode={}, failureMessage={}",
                        paymentData.failureCode(), paymentData.failureMessage());
                throw new BusinessException(OrderErrorType.PAYMENT_APPROVE_FAILED);
            }

            paymentKey = paymentData.paymentKey();

            log.info("[결제] 승인 완료 - orderId={}, paymentKey={}", orderId, paymentKey);

        } catch (FeignException e) {
            log.error("[결제] Feign 호출 실패 - orderId={}, status={}, message={}",
                    orderId, e.status(), e.contentUTF8(), e);
            throw new BusinessException(OrderErrorType.PAYMENT_APPROVE_FAILED);
        }


        // ===================== 7. OrderCreatedEvent 발행 =====================

        log.info("[이벤트] 발행 준비");
        log.info("[이벤트] - orderId: {}", orderId);
        log.info("[이벤트] - totalAmount: {}", savedOrder.getTotalPrice().getAmount());
        log.info("[이벤트] - requestPoint: {}", requestPoint);
        log.info("[이벤트] - usedCouponAmount: {}", usedCouponAmount);
        log.info("[이벤트] - amountPayable: {}", amountPayable);
        log.info("[이벤트] - pointReservationId: {}", pointReservationId);
        log.info("[이벤트] - couponReservationId: {}", couponReservationId);
        log.info("[이벤트] - paymentKey: {}", paymentKey);

        OrderCreatedEvent event = OrderCreatedEvent.of(
                orderId,
                savedOrder.getTotalPrice().getAmount(), // 주문 총액
                usedCouponAmount,   // 쿠폰 차감액
                requestPoint,   // 사용할 포인트
                amountPayable,  // 실제 결제 액수 (주문 총액 - 사용 포인트 - 쿠폰 차감액)
                request.methodType(),
                request.pgProvider(),
                request.currency(),
                request.couponId(),    // 쿠폰 예약 Id
                UUID.fromString(pointReservationId), // 포인트 예약 Id
                paymentKey
        );

        eventPublisher.publishExternal(event);


        log.info("[이벤트] 생성 완료 - event={}", event);

        try {
            eventPublisher.publishExternal(event);
            log.info("[이벤트] Kafka 발행 성공 - eventId={}", event.eventId());
        } catch (Exception e) {
            log.error("[이벤트] Kafka 발행 실패", e);
            throw e;
        }

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
