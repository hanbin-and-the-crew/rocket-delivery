package org.sparta.order.infrastructure.repository;

import lombok.RequiredArgsConstructor;
import org.sparta.order.domain.entity.Order;
import org.sparta.order.domain.repository.OrderRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Order Repository 구현체
 * QueryDSL을 활용한 동적 쿼리 처리
 */
@Repository
@RequiredArgsConstructor
public abstract class OrderRepositoryImpl implements OrderRepository {

    private final OrderJpaRepository orderJpaRepository;
//    private final JPAQueryFactory queryFactory;

    @Override
    public Order save(Order order) {
        return orderJpaRepository.save(order);
    }

    @Override
    public Optional<Order> findById(UUID id) {
        return orderJpaRepository.findById(id);
    }

    @Override
    public Optional<Order> findByIdAndDeletedAtIsNull(UUID id) {
        return orderJpaRepository.findByIdAndDeletedAtIsNull(id);
    }

//    @Override
//    public Page<Order> searchOrders(OrderSearchCondition condition, Pageable pageable) {
//        QOrder order = QOrder.order;
//        BooleanBuilder builder = new BooleanBuilder();
//
//        // Soft Delete 필터
//        builder.and(order.deletedAt.isNull());
//
//        // 동적 조건 추가
//        if (condition.supplierId() != null) {
//            builder.and(order.supplierId.eq(condition.supplierId()));
//        }
//        if (condition.receiptCompanyId() != null) {
//            builder.and(order.receiptCompanyId.eq(condition.receiptCompanyId()));
//        }
//        if (condition.productId() != null) {
//            builder.and(order.productId.eq(condition.productId()));
//        }
//        if (condition.status() != null) {
//            builder.and(order.orderStatus.eq(condition.status()));
//        }
//        if (condition.startDate() != null) {
//            builder.and(order.createdAt.goe(condition.startDate()));
//        }
//        if (condition.endDate() != null) {
//            builder.and(order.createdAt.loe(condition.endDate()));
//        }
//        if (condition.dueStartDate() != null) {
//            builder.and(order.dueAt.goe(condition.dueStartDate()));
//        }
//        if (condition.dueEndDate() != null) {
//            builder.and(order.dueAt.loe(condition.dueEndDate()));
//        }
//
//        // 쿼리 실행
//        List<Order> results = queryFactory
//                .selectFrom(order)
//                .where(builder)
//                .offset(pageable.getOffset())
//                .limit(pageable.getPageSize())
//                .orderBy(order.createdAt.desc())
//                .fetch();
//
//        // 전체 개수 조회
//        Long total = queryFactory
//                .select(order.count())
//                .from(order)
//                .where(builder)
//                .fetchOne();
//
//        return new PageImpl<>(results, pageable, total != null ? total : 0L);
//    }

    @Override
    public void delete(Order order) {
        orderJpaRepository.save(order);
    }
}