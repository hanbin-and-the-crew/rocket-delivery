package org.sparta.order.application.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.common.api.ApiResponse;
import org.sparta.common.error.BusinessException;
import org.sparta.order.application.dto.request.OrderRequest;
import org.sparta.order.application.dto.response.OrderResponse;
import org.sparta.order.domain.entity.Order;
import org.sparta.order.domain.enumeration.CanceledReasonCode;
import org.sparta.order.domain.error.OrderErrorType;
import org.sparta.order.domain.repository.OrderRepository;
import org.sparta.order.domain.vo.Money;
import org.sparta.order.domain.vo.Quantity;
import org.sparta.order.infrastructure.client.DeliveryClient;
import org.sparta.order.infrastructure.client.HubClient;
import org.sparta.order.infrastructure.client.ProductClient;
import org.sparta.order.infrastructure.client.UserClient;
import org.sparta.order.infrastructure.client.dto.*;
import org.sparta.order.infrastructure.event.OrderEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 주문 서비스
 * - 주문 생성, 수정, 조회, 취소, 삭제 등의 핵심 비즈니스 로직 처리
 * - Feign Client를 통해 다른 서비스와 통신
 * - Kafka를 통해 Product 서비스와 재고 이벤트 주고받기
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductClient productClient;
    private final HubClient hubClient;
    private final UserClient userClient;
    private final DeliveryClient deliveryClient;
    private final OrderEventPublisher eventPublisher;

    /**
     * 주문 생성
     * 1. 상품 정보 조회 (Product 서비스)
     * 2. 허브 정보 검증 (Hub 서비스)
     * 3. 주문 생성
     * 4. 재고 예약 이벤트 발행 (Kafka → Product 서비스)
     * 5. 배송 생성 (Delivery 서비스)
     */
    @Transactional
    public OrderResponse.Create createOrder(OrderRequest.Create request, UUID userId) {
        log.info("주문 생성 시작 - productId: {}, quantity: {}, userId: {}",
                request.productId(), request.quantity(), userId);

        // 1. 상품 정보 조회
        ProductDetailResponse product = getProductOrThrow(request.productId());

        // 2. 허브 정보 검증
        validateHub(request.supplierHubId());
        validateHub(request.receiptHubId());

        // 3. 주문 생성
        Order order = Order.create(
                request.supplierId(),
                request.supplierCompanyId(),
                request.supplierHubId(),
                request.receiptCompanyId(),
                request.receiptHubId(),
                request.productId(),
                product.name(),
                Money.of(product.price()),
                Quantity.of(request.quantity()),
                request.deliveryAddress(),
                request.dueAt(),
                request.requestedMemo()
        );

        Order savedOrder = orderRepository.save(order);

        // 4. 재고 예약 이벤트 발행 (Kafka)
        eventPublisher.publishOrderCreated(
                savedOrder.getId(),
                request.productId(),
                request.quantity(),
                userId
        );

        // 5. 배송 생성
        try {
            DeliveryCreateResponse delivery = createDelivery(savedOrder);
            savedOrder.setDeliveryId(delivery.deliveryId());
            orderRepository.save(savedOrder);
            log.info("배송 생성 완료 - orderId: {}, deliveryId: {}",
                    savedOrder.getId(), delivery.deliveryId());
        } catch (Exception e) {
            log.error("배송 생성 실패 - orderId: {}", savedOrder.getId(), e);
            // 배송 생성 실패 시에도 주문은 생성되도록 처리
            // 추후 배송을 수동으로 생성하거나 재시도 로직 추가 가능
        }

        log.info("주문 생성 완료 - orderId: {}", savedOrder.getId());
        return OrderResponse.Create.of(savedOrder);
    }

    /**
     * 주문 상세 조회
     */
    public OrderResponse.Detail getOrder(UUID orderId) {
        Order order = findByIdActiveOrThrow(orderId);
        return OrderResponse.Detail.of(order);
    }

//    /**
//     * 주문 목록 조회 (검색)
//     */
//    public Page<OrderResponse.Summary> searchOrders(
//            OrderSearchCondition condition,
//            Pageable pageable
//    ) {
//        Page<Order> orders = orderRepository.searchOrders(condition, pageable);
//        return orders.map(OrderResponse.Summary::of);
//    }

    /**
     * 주문 수량 변경
     * - PLACED 상태에서만 가능
     */
    @Transactional
    public OrderResponse.Update changeQuantity(
            UUID orderId,
            OrderRequest.ChangeQuantity request,
            UUID userId
    ) {
        log.info("주문 수량 변경 시작 - orderId: {}, newQuantity: {}, userId: {}",
                orderId, request.quantity(), userId);

        Order order = findByIdActiveOrThrow(orderId);
        order.changeQuantity(request.quantity(), userId);
        orderRepository.save(order);

        log.info("주문 수량 변경 완료 - orderId: {}, newQuantity: {}",
                orderId, request.quantity());

        return OrderResponse.Update.of(
                order,
                String.format("주문 수량이 %d개로 변경되었습니다", request.quantity())
        );
    }

    /**
     * 납품 기한 변경
     * - PLACED 상태에서만 가능
     */
    @Transactional
    public OrderResponse.Update changeDueAt(
            UUID orderId,
            OrderRequest.ChangeDueAt request,
            UUID userId
    ) {
        log.info("납품 기한 변경 시작 - orderId: {}, newDueAt: {}, userId: {}",
                orderId, request.dueAt(), userId);

        Order order = findByIdActiveOrThrow(orderId);
        order.changeDueAt(request.dueAt(), userId);
        orderRepository.save(order);

        log.info("납품 기한 변경 완료 - orderId: {}, newDueAt: {}",
                orderId, request.dueAt());

        return OrderResponse.Update.of(
                order,
                String.format("납품 기한이 %s로 변경되었습니다", request.dueAt())
        );
    }

    /**
     * 요청사항 변경
     * - PLACED 상태에서만 가능
     */
    @Transactional
    public OrderResponse.Update changeMemo(
            UUID orderId,
            OrderRequest.ChangeMemo request,
            UUID userId
    ) {
        log.info("요청사항 변경 시작 - orderId: {}, userId: {}", orderId, userId);

        Order order = findByIdActiveOrThrow(orderId);
        order.changeRequestedMemo(request.requestedMemo(), userId);
        orderRepository.save(order);

        log.info("요청사항 변경 완료 - orderId: {}", orderId);

        return OrderResponse.Update.of(order, "요청사항이 변경되었습니다");
    }

    /**
     * 주문 출고 처리
     */
    @Transactional
    public OrderResponse.Update dispatchOrder(
            UUID orderId,
            OrderRequest.Dispatch request,
            UUID userId
    ) {
        log.info("주문 출고 처리 시작 - orderId: {}, userId: {}", orderId, userId);

        Order order = findByIdActiveOrThrow(orderId);
        order.dispatch(orderId, userId, request.dispatchedAt());
        orderRepository.save(order);

        log.info("주문 출고 처리 완료 - orderId: {}", orderId);

        return OrderResponse.Update.of(order, "주문이 출고되었습니다");
    }

    /**
     * 주문 취소
     * - 재고 예약 취소 이벤트 발행
     */
    @Transactional
    public OrderResponse.Update cancelOrder(
            UUID orderId,
            OrderRequest.Cancel request,
            UUID userId
    ) {
        log.info("주문 취소 시작 - orderId: {}, userId: {}, reason: {}",
                orderId, userId, request.reasonCode());

        Order order = findByIdActiveOrThrow(orderId);

        // 취소 처리
        CanceledReasonCode reasonCode = CanceledReasonCode.valueOf(request.reasonCode());
        order.cancel(orderId, userId, reasonCode, request.reasonMemo());
        orderRepository.save(order);

        // 재고 예약 취소 이벤트 발행
        eventPublisher.publishOrderCancelled(
                orderId,
                order.getProductId(),
                order.getQuantity().getValue()
        );

        log.info("주문 취소 완료 - orderId: {}", orderId);

        return OrderResponse.Update.of(order, "주문이 취소되었습니다");
    }

    /**
     * 주문 삭제 (Soft Delete)
     */
    @Transactional
    public void deleteOrder(UUID orderId, String deletedBy) {
        log.info("주문 삭제 시작 - orderId: {}, deletedBy: {}", orderId, deletedBy);

        Order order = findByIdOrThrow(orderId);
        order.delete(deletedBy);
        orderRepository.delete(order);

        log.info("주문 삭제 완료 - orderId: {}", orderId);
    }

    /**
     * 재고 예약 실패 처리 (Kafka 이벤트 수신 시 호출)
     */
    @Transactional
    public void handleStockReservationFailed(UUID orderId, String reason) {
        log.warn("재고 예약 실패 처리 - orderId: {}, reason: {}", orderId, reason);

        Order order = findByIdOrThrow(orderId);

        // 주문을 자동으로 취소 처리
        if (order.isCancelable()) {
            order.cancel(
                    orderId,
                    null, // 시스템에 의한 자동 취소
                    CanceledReasonCode.OUT_OF_STOCK,
                    "재고 부족으로 인한 자동 취소: " + reason
            );
            orderRepository.save(order);
            log.info("재고 부족으로 주문 자동 취소 완료 - orderId: {}", orderId);
        }
    }

    // ========== Private 헬퍼 메서드 ==========

    private Order findByIdOrThrow(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(OrderErrorType.ORDER_NOT_FOUND));
    }

    private Order findByIdActiveOrThrow(UUID orderId) {
        return orderRepository.findByIdAndDeletedAtIsNull(orderId)
                .orElseThrow(() -> new BusinessException(OrderErrorType.ORDER_NOT_FOUND));
    }

    private ProductDetailResponse getProductOrThrow(UUID productId) {
        try {
            ApiResponse<ProductDetailResponse> response = productClient.getProduct(productId);
            if (response.data() == null) {
                throw new BusinessException(OrderErrorType.PRODUCT_NOT_FOUND);
            }
            return response.data();
        } catch (FeignException.NotFound e) {
            throw new BusinessException(OrderErrorType.PRODUCT_NOT_FOUND);
        } catch (Exception e) {
            log.error("상품 조회 실패 - productId: {}", productId, e);
            throw new BusinessException(OrderErrorType.PRODUCT_NOT_FOUND);
        }
    }

    private void validateHub(UUID hubId) {
        try {
            ApiResponse<HubResponse> response = hubClient.getHub(hubId);
            if (response.data() == null) {
                throw new BusinessException(OrderErrorType.HUB_NOT_FOUND);
            }
        } catch (FeignException.NotFound e) {
            throw new BusinessException(OrderErrorType.HUB_NOT_FOUND);
        } catch (Exception e) {
            log.error("허브 조회 실패 - hubId: {}", hubId, e);
            throw new BusinessException(OrderErrorType.HUB_NOT_FOUND);
        }
    }

    private DeliveryCreateResponse createDelivery(Order order) {
        try {
            DeliveryCreateRequest request = new DeliveryCreateRequest(
                    order.getId(),
                    order.getSupplierHubId(),
                    order.getReceiptHubId(),
                    order.getAddressSnapshot()
            );
            ApiResponse<DeliveryCreateResponse> response = deliveryClient.createDelivery(request);
            if (response.data() == null) {
                throw new BusinessException(OrderErrorType.DELIVERY_CREATE_FAILED);
            }
            return response.data();
        } catch (Exception e) {
            log.error("배송 생성 실패 - orderId: {}", order.getId(), e);
            throw new BusinessException(OrderErrorType.DELIVERY_CREATE_FAILED);
        }
    }
}