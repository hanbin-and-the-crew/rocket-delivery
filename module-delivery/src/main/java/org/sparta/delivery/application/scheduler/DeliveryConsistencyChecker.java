//package org.sparta.delivery.application.scheduler;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.sparta.delivery.domain.entity.Delivery;
//import org.sparta.delivery.domain.entity.DeliveryProcessedEvent;
//import org.sparta.delivery.domain.enumeration.DeliveryStatus;
//import org.sparta.delivery.domain.repository.DeliveryProcessedEventRepository;
//import org.sparta.delivery.domain.repository.DeliveryRepository;
//import org.sparta.delivery.infrastructure.client.OrderClient;
//import org.sparta.delivery.infrastructure.client.OrderFeignClient;
//import org.sparta.deliverylog.application.service.DeliveryLogService;
//import org.sparta.deliveryman.application.service.DeliveryManService;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.UUID;
//
///**
// * 배송 데이터 정합성 체크 스케줄러
// *
// * 목적: 주문 취소 이벤트와 배송 생성 타이밍 이슈로 인한 유령 배송 방지
// *
// * 시나리오:
// * 1. Order 생성 → OrderApprovedEvent 발행
// * 2. 동시에 Order 취소 → OrderCancelledEvent 발행
// * 3. OrderCancelledEvent가 먼저 도착 → Delivery 없음 (취소 실패)
// * 4. OrderApprovedEvent 도착 → Delivery 생성
// * 5. 결과: Order는 CANCELLED, Delivery는 CREATED (유령 배송!)
// *
// * 해결: 매시간 Order 상태 확인하여 불일치 발견 시 보정
// */
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class DeliveryConsistencyChecker {
//
//    private final DeliveryRepository deliveryRepository;
//    private final DeliveryProcessedEventRepository deliveryProcessedEventRepository;
//    private final DeliveryLogService deliveryLogService;
//    private final DeliveryManService deliveryManService;
//    private final OrderFeignClient orderClient;
//
//    // 한 번에 처리할 배송 건수 (메모리 절약)
//    private static final int BATCH_SIZE = 100;
//
//    /**
//     * 매시간 정각 실행: 최근 24시간 내 생성된 배송 중 Order와 상태 불일치 체크
//     * - 성능 고려: 배치 단위로 처리
//     * - Feign 타임아웃 고려: 개별 호출로 실패 격리
//     */
//    @Scheduled(cron = "0 0 * * * *") // 매시간 정각
//    @Transactional
//    public void checkAndFixCancelledOrders() {
//        log.info("[Scheduler] Starting delivery consistency check for cancelled orders");
//
//        try {
//            LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
//
//            // CANCELED가 아닌 배송만 조회 (성능 최적화)
//            List<Delivery> activeDeliveries = deliveryRepository
//                    .findByStatusNotAndCreatedAtAfterAndDeletedAtIsNull(
//                            DeliveryStatus.CANCELED,
//                            oneDayAgo
////                            PageRequest.of(0, BATCH_SIZE) // 현재 해당 메소드는 이 세번째 인자를 필요로하지 않음
//                                                            // 이 옵션도 사용하려면 기존 메서드에 인자를 추가해서 관련 메소드들을 변경하든, 메소드를 하나 더 만들든 해야 됨
//                    );
//
//            if (activeDeliveries.isEmpty()) {
//                log.info("[Scheduler] No active deliveries found. Nothing to check.");
//                return;
//            }
//
//            log.info("[Scheduler] Found {} active deliveries created after {}",
//                    activeDeliveries.size(), oneDayAgo);
//
//            int fixedCount = 0;
//            int skippedCount = 0;
//            int alreadyCancelledCount = 0;
//
//            for (Delivery delivery : activeDeliveries) {
//                try {
//                    // Order 상태 조회
//                    OrderFeignClient.OrderStatus orderStatus =
//                            orderClient.getOrder(delivery.getOrderId()).data().orderStatus();
//
//                    // Order가 취소되었는데 Delivery는 아직 활성 상태
//                    if ("CANCELLED".equals(orderStatus.toString()) &&
//                            delivery.getStatus() != DeliveryStatus.CANCELED) {
//
//                        log.warn("[Scheduler] Found inconsistent delivery: " +
//                                        "deliveryId={}, orderId={}, " +
//                                        "Order.status=CANCELLED, Delivery.status={}",
//                                delivery.getId(), delivery.getOrderId(), delivery.getStatus());
//
//                        // 보상 처리
//                        fixDelivery(delivery);
//                        fixedCount++;
//                    } else {
//                        log.trace("[Scheduler] Delivery is consistent: deliveryId={}, status={}",
//                                delivery.getId(), delivery.getStatus());
//                    }
//
//                } catch (feign.FeignException.NotFound e) {
//                    log.warn("[Scheduler] Order not found: orderId={}, deliveryId={} - Marking delivery as cancelled",
//                            delivery.getOrderId(), delivery.getId());
//
//                    // Order가 삭제된 경우 Delivery도 취소
//                    fixDelivery(delivery);
//                    fixedCount++;
//
//                } catch (feign.FeignException e) {
//                    log.error("[Scheduler] Failed to get order status: orderId={}, status={}",
//                            delivery.getOrderId(), e.status(), e);
//                    skippedCount++;
//
//                } catch (Exception e) {
//                    log.error("[Scheduler] Failed to check delivery consistency: " +
//                                    "deliveryId={}, orderId={}",
//                            delivery.getId(), delivery.getOrderId(), e);
//                    skippedCount++;
//                }
//            }
//
//            log.info("[Scheduler] Delivery consistency check completed: " +
//                            "checked={}, fixed={}, skipped={}",
//                    activeDeliveries.size(), fixedCount, skippedCount);
//
//        } catch (Exception e) {
//            log.error("[Scheduler] Failed to run delivery consistency check", e);
//        }
//    }
//
//    /**
//     * 불일치 배송 보정 (멱등성 보장)
//     * - Delivery 취소
//     * - DeliveryLog 전체 취소
//     * - DeliveryMan 할당 해제
//     * - 처리 이벤트 기록 (중복 처리 방지)
//     */
//    private void fixDelivery(Delivery delivery) {
//        String eventId = "SCHEDULER_FIX_" + delivery.getId() + "_" + System.currentTimeMillis();
//
//        // 멱등성 체크: 이미 스케줄러가 처리했는지 확인
//        if (deliveryProcessedEventRepository.existsByEventId(eventId)) {
//            log.info("[Scheduler] Delivery already fixed: deliveryId={}", delivery.getId());
//            return;
//        }
//
//        log.info("[Scheduler] Fixing inconsistent delivery: deliveryId={}, orderId={}",
//                delivery.getId(), delivery.getOrderId());
//
//        try {
//            // 1. Delivery 취소 (이미 취소된 경우 무시)
//            if (delivery.getStatus() != DeliveryStatus.CANCELED) {
//                delivery.cancel();
//                log.info("[Scheduler] Delivery cancelled: deliveryId={}", delivery.getId());
//            }
//
//            // 2. DeliveryLog 전체 취소
//            try {
//                deliveryLogService.cancelAllLogsByDeliveryId(delivery.getId());
//                log.info("[Scheduler] All DeliveryLogs cancelled: deliveryId={}",
//                        delivery.getId());
//            } catch (Exception e) {
//                log.error("[Scheduler] Failed to cancel DeliveryLogs: deliveryId={}",
//                        delivery.getId(), e);
//                // 로그 취소 실패해도 계속 진행
//            }
//
//            // 3. 배송 담당자 할당 해제
//            if (delivery.getHubDeliveryManId() != null) {
//                try {
//                    deliveryManService.unassignDelivery(delivery.getHubDeliveryManId());
//                    log.info("[Scheduler] DeliveryMan unassigned: " +
//                                    "deliveryManId={}, deliveryId={}",
//                            delivery.getHubDeliveryManId(), delivery.getId());
//                } catch (Exception e) {
//                    log.error("[Scheduler] Failed to unassign DeliveryMan: " +
//                                    "deliveryManId={}, deliveryId={}",
//                            delivery.getHubDeliveryManId(), delivery.getId(), e);
//                    // 담당자 해제 실패해도 계속 진행
//                }
//            }
//
//            // 4. 처리 완료 기록 (중복 처리 방지)
//            deliveryProcessedEventRepository.save(
//                    DeliveryProcessedEvent.of(eventId, "SCHEDULER_CONSISTENCY_FIX")
//            );
//
//            log.info("[Scheduler] Delivery fix completed: deliveryId={}", delivery.getId());
//
//        } catch (Exception e) {
//            log.error("[Scheduler] Failed to fix delivery: deliveryId={}",
//                    delivery.getId(), e);
//            throw e; // 트랜잭션 롤백
//        }
//    }
//}
