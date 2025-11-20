package org.sparta.order.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.common.api.ApiResponse;
import org.sparta.common.error.BusinessException;
import org.sparta.common.event.EventPublisher;
import org.sparta.order.application.dto.request.OrderRequest;
import org.sparta.order.application.dto.response.OrderResponse;
import org.sparta.order.domain.entity.Order;
import org.sparta.order.domain.enumeration.CanceledReasonCode;
import org.sparta.order.domain.enumeration.OrderStatus;
import org.sparta.order.domain.error.OrderErrorType;
import org.sparta.order.domain.repository.OrderRepository;
import org.sparta.order.domain.vo.Money;
import org.sparta.order.domain.vo.Quantity;
import org.sparta.order.infrastructure.client.*;
import org.sparta.order.infrastructure.client.dto.request.*;
import org.sparta.order.infrastructure.client.dto.response.*;
import org.sparta.order.infrastructure.event.publisher.OrderCanceledEvent;
import org.sparta.order.infrastructure.event.publisher.OrderCreatedSpringEvent;
import org.sparta.order.infrastructure.event.publisher.PaymentCompletedEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
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
    private final DeliveryLogClient deliveryLogClient;

    private final EventPublisher orderEventPublisher; // Kafka 이벤트 퍼블리셔
    // 이것도 코드 안정화되면 이벤트 퍼블리셔 합칠 예정

    private final ObjectMapper objectMapper;    // kafka 직렬화 문제 때문에 생성

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

        // 3. 주문 생성
        Order order = Order.create(
                request.supplierId(),
                request.supplierCompanyId(),
                request.supplierHubId(),
                request.receiptCompanyId(),
                request.receiptHubId(),
                request.productId(),
                "temp",//product.name(),
                Money.of(10000L),//Money.of(product.price()),
                Quantity.of(request.quantity()),
                request.deliveryAddress(),
                request.userName(),
                request.userPhoneNumber(),
                request.slackId(),
                request.dueAt(),
                request.requestedMemo()
        );

        Order savedOrder = orderRepository.save(order);

        /**
         *
         * EventListener STEP1. 주문 생성 이후 OrderCreatedEvent를 발행
         *
         */
        orderEventPublisher.publishLocal(OrderCreatedSpringEvent.of(savedOrder, userId));

        log.info("주문 생성 완료 - orderId: {}", savedOrder.getId());
        return OrderResponse.Create.of(savedOrder);
    }

    /**
     * 주문 상세 조회
     */
    //TODO:배송 담당자 -> 본인담당 주문건만
    public OrderResponse.Detail getOrder(UUID orderId, UUID userId) {
        Order order = findByIdActiveOrThrow(orderId);
        if (!order.getSupplierId().equals(userId)) {
            throw new BusinessException(OrderErrorType.UNAUTHORIZED_USER_SUPPLIER_ID);
        }
        // TODO: 마스터 / 허브 관리자(본인담당) 가능
        return OrderResponse.Detail.of(order);
    }

//    //TODO: 작성자 본인 / 배송 담당자 -> 본인담당 주문건만 / 모든 사용자 가능
//    //TODO: repo 수정
    /**
     * 주문 목록 조회 (페이징만)
     */
    @Transactional(readOnly = true)
    public Page<OrderResponse.Summary> searchOrders(
            Pageable pageable,
            UUID userId
    ) {
        log.info("주문 목록 조회 시작 - userId: {}", userId);

        // TODO: 권한별 필터링 추가 필요
        // - 작성자 본인: 본인 주문만
        // - 배송 담당자: 본인 담당 주문만
        // - 마스터/허브 관리자: 전체 or 담당 허브

        Page<Order> orders = orderRepository.searchOrders(null, pageable);
        return orders.map(OrderResponse.Summary::of);
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
        if (order.getOrderStatus() != OrderStatus.PLACED) {
            throw new BusinessException(OrderErrorType.CANNOT_MODIFY_NOT_PLACED_ORDER);
        }

        if (!order.getSupplierId().equals(userId)) {
            throw new BusinessException(OrderErrorType.UNAUTHORIZED_USER_SUPPLIER_ID);
        }
        // TODO: 마스터 / 허브 관리자(본인담당) 가능

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
        if (order.getOrderStatus() != OrderStatus.PLACED) {
            throw new BusinessException(OrderErrorType.CANNOT_MODIFY_NOT_PLACED_ORDER);
        }

        if (!order.getSupplierId().equals(userId)) {
            throw new BusinessException(OrderErrorType.UNAUTHORIZED_USER_SUPPLIER_ID);
        }
        // TODO: 마스터 / 허브 관리자(본인담당) 가능

        order.changeRequestedMemo(request.requestedMemo(), userId);
        orderRepository.save(order);

        log.info("요청사항 변경 완료 - orderId: {}", orderId);

        return OrderResponse.Update.of(order, "요청사항이 변경되었습니다");
    }

    /**
     * 주소 변경
     * - PLACED 상태에서만 가능
     */
    @Transactional
    public OrderResponse.Update changeAddress(
            UUID orderId,
            OrderRequest.ChangeAddress request,
            UUID userId
    ) {
        log.info("주소 변경 시작 - orderId: {}, userId: {}", orderId, userId);

        Order order = findByIdActiveOrThrow(orderId);
        if (order.getOrderStatus() != OrderStatus.PLACED) {
            throw new BusinessException(OrderErrorType.CANNOT_MODIFY_NOT_PLACED_ORDER);
        }

        if (!order.getSupplierId().equals(userId)) {
            throw new BusinessException(OrderErrorType.UNAUTHORIZED_USER_SUPPLIER_ID);
        }
        // TODO: 마스터 / 허브 관리자(본인담당) 가능 / 권한 체크 메서드 생성하기
//        UserResponse userResponse = getUser(userId);

        order.changeAddress(request.addressSnapshot(), userId);
        orderRepository.save(order);

        log.info("주소 변경 완료 - orderId: {}", orderId);

        return OrderResponse.Update.of(order, "주소가 변경되었습니다");
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

        // TODO: 마스터 / 허브 관리자(담당)만 가능

        Order order = findByIdActiveOrThrow(orderId);
        if (order.getOrderStatus() != OrderStatus.PLACED) {
            throw new BusinessException(OrderErrorType.CANNOT_MODIFY_NOT_PLACED_ORDER);
        }

        order.dispatch(orderId, userId, request.dispatchedAt());
        Order savedOrder = orderRepository.save(order);

        // 출고 완료, Product 서비스에 재고 감소 이벤트 발행
        // 현재 주문 생성에도 똑같이 되어 있어서 나중에 정리되면 둘 중 하나는 제거할 것
        orderEventPublisher.publishExternal(PaymentCompletedEvent.of(savedOrder));
        
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
        if (!order.getSupplierId().equals(userId)) {
            throw new BusinessException(OrderErrorType.UNAUTHORIZED_USER_SUPPLIER_ID);
        }
        // TODO: 마스터 / 허브 관리자(본인담당) 가능

        // 주문 취소 처리
        CanceledReasonCode reasonCode = CanceledReasonCode.valueOf(request.reasonCode());
        order.cancel(orderId, userId, reasonCode, request.reasonMemo());
        Order canceledOrder = orderRepository.save(order);

        // TODO : 배송, 배송 로그 삭제 처리 이벤트

        // 재고 예약 취소 이벤트 발행
        orderEventPublisher.publishExternal(OrderCanceledEvent.of(canceledOrder));

        order.delete(userId);
        orderRepository.delete(order);

        log.info("주문 취소 완료 - orderId: {}", orderId);

        return OrderResponse.Update.of(order, "주문이 취소되었습니다");
    }

    /**
     * 주문 삭제 (Soft Delete)
     */
    @Transactional
    public void deleteOrder(UUID orderId, UUID userId) {
        log.info("주문 삭제 시작 - orderId: {}, deletedBy: {}", orderId, userId);

        Order order = findByIdOrThrow(orderId);

        if (!order.getSupplierId().equals(userId)) {
            throw new BusinessException(OrderErrorType.UNAUTHORIZED_USER_SUPPLIER_ID);
        }
        // TODO: 마스터 / 허브 관리자(본인담당) 가능

        order.delete(userId);
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

    // 배송 생성
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


    /**
     * 배송로그 생성
     */
    private DeliveryLogCreateResponse createDeliveryLog(Order order) {
        try {
            DeliveryLogCreateRequest logRequest = new DeliveryLogCreateRequest(
                    order.getId(),
                    order.getSupplierHubId(),
                    order.getReceiptHubId(),
                    order.getAddressSnapshot()
            );
            ApiResponse<DeliveryLogCreateResponse> logResponse = deliveryLogClient.createDeliveryLog(logRequest);
            if (logResponse == null || logResponse.data() == null) {
                throw new BusinessException(OrderErrorType.DELIVERY_LOG_CREATE_FAILED);
            }
            return logResponse.data();
        } catch (Exception e) {
            log.error("배송로그 생성 실패 - orderId: {}", order.getId(), e);
            throw new BusinessException(OrderErrorType.DELIVERY_LOG_CREATE_FAILED);
        }
    }

    /**
     * 배송에 배송로그 ID 저장
     */
    private void saveDeliveryLog(Order order, UUID deliveryLogId, UUID userId) {
        try {
            DeliveryCreateRequest request = new DeliveryCreateRequest(
                    order.getId(),
                    order.getSupplierHubId(),
                    order.getReceiptHubId(),
                    order.getAddressSnapshot()
            );
            ApiResponse<DeliveryCreateResponse> response = deliveryClient.saveDeliveryLog(deliveryLogId, request);
            if (response.data() == null) {
                throw new BusinessException(OrderErrorType.DELIVERY_LOG_SAVE_FAILED);
            }
        } catch (Exception e) {
            log.error("배송에 배송로그 ID 저장 실패 - orderId: {}, deliveryLogId: {}", order.getId(), deliveryLogId, e);
            throw new BusinessException(OrderErrorType.DELIVERY_LOG_SAVE_FAILED);
        }
    }

    /**
     * 배송 삭제
     */
    private void deleteDelivery(UUID deliveryId, UUID userId) {
        try {
            Map<String, UUID> requestMap = Map.of("userId", userId);
            ApiResponse<Void> response = deliveryClient.deleteDelivery(
                    deliveryId,
                    requestMap
            );
            if (response == null || response.data() == null) {
                throw new BusinessException(OrderErrorType.DELIVERY_DELETE_FAILED);
            }
        } catch (Exception e) {
            log.error("배송 삭제 실패 - deliveryId: {}, userId: {}", deliveryId, userId, e);
            throw new BusinessException(OrderErrorType.DELIVERY_DELETE_FAILED);
        }
    }

    /**
     * 사용자 권한 확인
     * */
    private UserResponse getUser(UUID userId) {
        ApiResponse<UserResponse> response = userClient.getSpecificUserInfo(userId);
        if (response.data() == null) {
            throw new BusinessException(OrderErrorType.USER_NOT_FOUND);
        }
        return response.data();
    }
}