package org.sparta.delivery.domain.repository;

import org.sparta.delivery.domain.entity.Delivery;
import org.sparta.delivery.domain.enumeration.DeliveryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryRepository {

    Delivery save(Delivery delivery);

    Optional<Delivery> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsByOrderIdAndDeletedAtIsNull(UUID orderId);

    Page<Delivery> search(
            DeliveryStatus status,
            UUID hubId,
            UUID companyId,
            Pageable pageable,
            Sort.Direction direction
    );

    Optional<Delivery> findByOrderIdAndDeletedAtIsNull(UUID orderId);

    /**
     * 스케줄러용: 특정 상태가 아니고 특정 시간 이후 생성된 배송 조회
     * @param status 제외할 상태
     * @param createdAfter 이 시간 이후 생성된 것만
     * @return Delivery 목록
     */
    List<Delivery> findByStatusNotAndCreatedAtAfterAndDeletedAtIsNull(
            DeliveryStatus status,
            LocalDateTime createdAfter
    );
}
