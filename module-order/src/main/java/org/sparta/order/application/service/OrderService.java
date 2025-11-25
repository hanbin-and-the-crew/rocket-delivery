package org.sparta.order.application.service;

import lombok.RequiredArgsConstructor;
import org.sparta.common.error.BusinessException;
import org.sparta.common.event.EventPublisher;
import org.sparta.order.application.dto.request.OrderRequest;
import org.sparta.order.application.dto.response.OrderResponse;
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

    /**
     * Retrieve the non-deleted Order with the specified id or throw if not found.
     *
     * @param orderId the UUID of the order to retrieve
     * @return the matching Order
     * @throws BusinessException if no non-deleted order exists for the given id (OrderErrorType.ORDER_NOT_FOUND)
     */
    private Order findOrderOrThrow(UUID orderId) {
        return orderRepository.findByIdAndDeletedAtIsNull(orderId)
                .orElseThrow(() -> new BusinessException(OrderErrorType.ORDER_NOT_FOUND));
    }

    /**
     * Normalize a Pageable to an allowed page size and a defined sort.
     *
     * If the requested page size is not one of the allowed values (10, 30, 50), the default size (10) is used.
     * If no sort is provided or the sort is unsorted, the result is sorted by `createdAt` descending.
     *
     * @param pageable the original pageable containing page number, size, and sort
     * @return a PageRequest with the (possibly corrected) page number, size, and sort
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

    // ===== 주문 생성 =====

    /**
     * Create and persist a new order for the given customer.
     *
     * Publishes an external OrderCreatedEvent after saving; the new order is created in the CREATED state.
     *
     * @param customerId the customer's UUID placing the order
     * @param request    the creation payload containing product, quantity, address, due date, and contact information
     * @return           a detailed representation of the persisted order
     */
    public OrderResponse.Detail createOrder(UUID customerId, OrderRequest.Create request) {
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
     * Move the order identified by {@code orderId} to the approved state and publish an OrderApprovedEvent.
     *
     * @param orderId the identifier of the order to approve
     * @throws BusinessException if the order with the given id is not found
     */
    public void approveOrder(UUID orderId) {
        Order order = findOrderOrThrow(orderId);

        order.approve();

        // OrderApprovedEvent 발행 (배송/Slack 모듈 사용)
        // TODO: 필요한 정보 확인 후 추가
        eventPublisher.publishExternal(OrderApprovedEvent.of(order));
    }

    /**
     * Cancel an order and publish an OrderCancelledEvent to trigger downstream compensating actions.
     *
     * @param request a cancel request containing the target orderId, cancellation reasonCode, and optional reasonMemo
     * @return an Update response containing the updated order and a cancellation message
     * @throws BusinessException if the order does not exist or the provided cancellation reasonCode is invalid
     */
    public OrderResponse.Update cancelOrder(OrderRequest.Cancel request) {
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
    /**
     * Marks the specified order as shipped.
     *
     * @param request the ship request containing the target order's identifier
     * @return an OrderResponse.Update representing the updated order and a shipping message
     */
    public OrderResponse.Update shipOrder(OrderRequest.ShipOrder request) {
        Order order = findOrderOrThrow(request.orderId());
        order.markShipped();
        return OrderResponse.Update.of(order, "주문이 출고(배송 시작) 처리되었습니다.");
    }

    // 배송 완료 처리
    /**
     * Marks the specified order as delivered and returns an update response.
     *
     * @param request the deliver-order request containing the target order's identifier
     * @return an OrderResponse.Update containing the updated order and a delivery confirmation message
     */
    public OrderResponse.Update deliverOrder(OrderRequest.DeliverOrder request) {
        Order order = findOrderOrThrow(request.orderId());
        order.markDelivered();
        return OrderResponse.Update.of(order, "주문이 배송 완료 처리되었습니다.");
    }

    /**
     * Mark an order as deleted after validating it can be deleted.
     *
     * @param request the delete request containing the target order's id
     * @return an Update response containing the updated order and a deletion message
     */
    public OrderResponse.Update deleteOrder(OrderRequest.DeleteOrder request) {
        Order order = findOrderOrThrow(request.orderId());
        order.validateDeletable();
        order.markAsDeleted();
        return OrderResponse.Update.of(order, "주문이 삭제되었습니다.");
    }

    // ===== 주문 조회 =====

    /**
     * Retrieves detailed information for a single order.
     *
     * @param orderId the UUID of the order to retrieve
     * @return an OrderResponse.Detail representing the requested order
     */
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
    /**
     * Retrieve a paginated list of a customer's non-deleted orders as summary DTOs.
     *
     * @param customerId the customer's UUID whose orders are requested
     * @param pageable   paging and sorting parameters; page size and sort will be normalized to service defaults
     * @return           a page of OrderResponse.Summary representing the customer's non-deleted orders
     */
    @Transactional(readOnly = true)
    public Page<OrderResponse.Summary> getOrdersByCustomer(UUID customerId, Pageable pageable) {
        Pageable normalized = normalizePageable(pageable);
        return orderRepository.findByCustomerIdAndDeletedAtIsNull(customerId, normalized)
                .map(OrderResponse.Summary::from);
    }

    /**
     * Updates the due date of an existing order.
     *
     * @param orderId the identifier of the order to update
     * @param request request containing the new due date
     * @return an Update response containing the updated order and a confirmation message
     */

    public OrderResponse.Update changeDueAt(UUID orderId, OrderRequest.ChangeDueAt request) {
        Order order = findOrderOrThrow(orderId);
        order.changeDueAt(request.dueAt());
        return OrderResponse.Update.of(order, "납기일이 변경되었습니다.");
    }

    /**
     * Updates an order's delivery address.
     *
     * @param orderId the UUID of the order to update
     * @param request the request containing the new address snapshot
     * @return the updated OrderResponse.Update reflecting the order after the address change with a confirmation message
     */
    public OrderResponse.Update changeAddress(UUID orderId, OrderRequest.ChangeAddress request) {
        Order order = findOrderOrThrow(orderId);
        order.changeAddress(request.addressSnapshot());
        return OrderResponse.Update.of(order, "주소가 변경되었습니다.");
    }

    /**
     * Update an order's requested memo.
     *
     * Changes the requested memo on the specified order and returns an update response containing the modified order and a confirmation message.
     *
     * @param orderId the UUID of the order to update
     * @param request the change request containing the new requested memo
     * @return an OrderResponse.Update containing the updated order and a confirmation message
     */
    public OrderResponse.Update changeRequestMemo(UUID orderId, OrderRequest.ChangeMemo request) {
        Order order = findOrderOrThrow(orderId);
        order.changeRequestMemo(request.requestedMemo());
        return OrderResponse.Update.of(order, "요청사항이 변경되었습니다.");
    }
}