package org.sparta.delivery.domain.repository;

import org.sparta.delivery.domain.entity.Delivery;
import org.sparta.delivery.domain.enumeration.DeliveryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface DeliveryRepository {

    /**
     * 기본 저장 메서드
     */
    Delivery save(Delivery delivery);

    /**
     * soft delete 되지 않은 배송 단건 조회
     */
    Optional<Delivery> findByIdAndDeletedAtIsNull(UUID id);

    /**
     * soft delete 되지 않은 주문 연계 배송 조회
     * - 주문 승인/취소 이벤트 처리 시 사용
     */
    Optional<Delivery> findByOrderIdAndDeletedAtIsNull(UUID orderId);

    /**
     * 주문 기준 배송 존재 여부 체크
     * - 주문 승인 시, 이미 생성된 배송이 있는지 중복 검증 용도
     */
    boolean existsByOrderIdAndDeletedAtIsNull(UUID orderId);

    /**
     * 상태 기준 목록 조회 (soft delete 제외)
     * - GET /api/deliveries?status=... 페이징 조회
     */
    Page<Delivery> findAllByStatusAndDeletedAtIsNull(DeliveryStatus status, Pageable pageable);

    /**
     * 전체 활성 배송 목록 조회 (soft delete 제외)
     * - 관리용/통계용 등 기본 리스트
     */
    Page<Delivery> findAllByDeletedAtIsNull(Pageable pageable);


}
