package org.sparta.product.presentation;

import lombok.RequiredArgsConstructor;
import org.sparta.common.api.ApiResponse;
import org.sparta.product.application.service.ProductOutboxAdminService;
import org.sparta.product.domain.outbox.ProductOutboxEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Product Outbox(DLQ) 조회용 Admin 컨트롤러
 *
 * - 운영/디버깅용으로 FAILED 상태 Outbox 이벤트를 조회한다.
 * - Gateway 기준 경로 예시: /api/product/outbox/failed
 */
@RestController
@RequestMapping("/api/product/outbox")
@RequiredArgsConstructor
public class ProductOutboxAdminController {

    private final ProductOutboxAdminService outboxAdminService;

    /**
     * FAILED(DLQ) Outbox 이벤트 조회
     * 예) GET /api/product/outbox/failed?limit=100
     */
    @GetMapping("/failed")
    public ApiResponse<List<ProductOutboxEvent>> getFailedOutboxEvents(
            @RequestParam(name = "limit", defaultValue = "100") int limit
    ) {
        List<ProductOutboxEvent> events = outboxAdminService.getFailedEvents(limit);
        return ApiResponse.success(events);
    }
}
