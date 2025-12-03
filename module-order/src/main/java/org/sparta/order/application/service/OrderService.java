package org.sparta.order.application.service;

import lombok.RequiredArgsConstructor;
import org.sparta.common.error.BusinessException;
import org.sparta.common.event.EventPublisher;
import org.sparta.order.application.command.OrderCommand;
import org.sparta.order.application.error.OrderApplicationErrorType;
import org.sparta.order.presentation.dto.request.OrderRequest;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final EventPublisher eventPublisher; // Kafka + Spring Event 래퍼 (이미 common 모듈에 있음)

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

        orderRepository.save(order);

        // OrderCreatedEvent 발행 (재고 예약-> 성공 -> 결제 외부 연동 시작점)
        eventPublisher.publishExternal(OrderCreatedEvent.of(order));

        return OrderResponse.Detail.from(order);
    }

    // ===== 주문 상태 변경 처리 =====
    // 결제 성공 -> 재고 감소 -> 감소 성공 -> approveOrder()
    /**
     * stock에서 "재고 감소 완료" 이벤트가 온 뒤 호출된다고 가정.
     * - CREATED → APPROVED
     */
    public void approveOrder(UUID orderId) {
        Order order = findOrderOrThrow(orderId);

        order.approve();

        // OrderApprovedEvent 발행 (배송/Slack 모듈 사용)
        // TODO: 필요한 정보 확인 후 추가
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

    // 배송 시작/출고 처리
    // TODO: 배송 시작 이벤트 수신해서 메소드 실행 + 마스터,허브 관리자 가능
    public OrderResponse.Update shipOrder(OrderCommand.ShipOrder request) {
        Order order = findOrderOrThrow(request.orderId());
        order.markShipped();
        return OrderResponse.Update.of(order, "주문이 출고(배송 시작) 처리되었습니다.");
    }

    // 배송 완료 처리
    // TODO: 배송 완료 이벤트 수신해서 메소드 실행 + 마스터,허브 관리자 가능
    public OrderResponse.Update deliverOrder(OrderCommand.DeliverOrder request) {
        Order order = findOrderOrThrow(request.orderId());
        order.markDelivered();
        return OrderResponse.Update.of(order, "주문이 배송 완료 처리되었습니다.");
    }

    // 삭제
    public OrderResponse.Update deleteOrder(OrderCommand.DeleteOrder request) {
        Order order = findOrderOrThrow(request.orderId());
        order.validateDeletable();
        order.markAsDeleted();
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
    // TODO: 역할 분리 추가 예정
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
