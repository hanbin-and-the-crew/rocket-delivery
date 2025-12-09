package org.sparta.product.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.product.domain.outbox.ProductOutboxEvent;
import org.sparta.product.domain.repository.ProductOutboxEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Product Outbox(DLQ) 관리용 서비스
 *
 * - FAILED 상태의 Outbox 이벤트를 조회하여
 *   운영/디버깅 시 어떤 이벤트가 DLQ로 떨어져 있는지 확인하는 용도
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductOutboxAdminService {

    private final ProductOutboxEventRepository outboxRepository;

    /**
     * FAILED 상태 Outbox 이벤트 조회
     * @param limit 최대 조회 개수 (예: 100)
     */
    public List<ProductOutboxEvent> getFailedEvents(int limit) {
        int batchSize = Math.max(1, Math.min(limit, 500));  // 너무 큰 값 방지
        List<ProductOutboxEvent> events = outboxRepository.findFailedEvents(batchSize);

        log.debug("[ProductOutboxAdminService] FAILED outbox events 조회 - size={}", events.size());
        return events;
    }
}
