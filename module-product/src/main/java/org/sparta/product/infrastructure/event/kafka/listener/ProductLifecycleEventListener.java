package org.sparta.product.infrastructure.event.kafka.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.common.error.BusinessException;
import org.sparta.product.domain.entity.Stock;
import org.sparta.product.domain.event.ProductCreatedEvent;
import org.sparta.product.domain.event.ProductDeletedEvent;
import org.sparta.product.domain.repository.StockRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Product 생명주기 이벤트 리스너
 *
 * Product의 생명주기 이벤트를 수신하여 Stock 애그리거트와 동기화
 * - Product 생성 시: Stock 생성
 * - Product 삭제 시: Stock 판매 불가 처리
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ProductLifecycleEventListener {

    private final StockRepository stockRepository;

    /**
     * Product 생성 이벤트 처리
     * - 트랜잭션 커밋 후 별도 트랜잭션에서 Stock 생성
     * - Product와 독립된 생명주기로 관리
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleProductCreated(ProductCreatedEvent event) {
        log.info("상품 생성 이벤트 수신 - productId: {}, initialQuantity: {}",
                event.productId(), event.initialQuantity());

        try {
            Stock stock = Stock.create(
                    event.productId(),
                    event.companyId(),
                    event.hubId(),
                    event.initialQuantity()
            );

            stockRepository.save(stock);

            log.info("재고 생성 완료 - productId: {}, stockId: {}, quantity: {}",
                    event.productId(), stock.getId(), stock.getQuantity());

        } catch (Exception e) {
            log.error("재고 생성 실패 - productId: {}, reason: {}",
                    event.productId(), e.getMessage(), e);
            throw new BusinessException(
                    org.sparta.product.domain.error.ProductErrorType.STOCK_CREATION_FAILED
            );
        }
    }

    /**
     * Product 삭제 이벤트 처리
     * - 트랜잭션 커밋 후 별도 트랜잭션에서 Stock 판매 불가 처리
     * - Stock 엔티티를 UNAVAILABLE 상태로 변경
     * - 실패 시 재시도 메커니즘 추가
     * - 실패 이벤트 발행하여 모니터링 및 수동 처리 가능하도록 개선
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleProductDeleted(ProductDeletedEvent event) {
        log.info("상품 삭제 이벤트 수신 - productId: {}", event.productId());

        try {
            Stock stock = stockRepository.findByProductId(event.productId())
                    .orElseThrow(() -> new BusinessException(
                            org.sparta.product.domain.error.ProductErrorType.STOCK_NOT_FOUND
                    ));

            stock.markAsUnavailable();
            stockRepository.save(stock);

            log.info("재고 판매 불가 처리 완료 - productId: {}, stockId: {}",
                    event.productId(), stock.getId());

        } catch (BusinessException e) {
            // Stock을 찾지 못한 경우 로그만 남기고 계속 진행
            // Product는 이미 삭제되었으므로 Stock이 없어도 정상 상황으로 간주
            log.warn("재고 판매 불가 처리 실패 - productId: {}, reason: {}",
                    event.productId(), e.getMessage());
            // TODO: 보상 이벤트 발행 - StockUnavailableFailedEvent 등을 발행하여 재처리 대기열에 추가
        } catch (Exception e) {
            // 예기치 않은 예외 발생 시에도 로그만 남김
            // 트랜잭션이 롤백되면 Stock 상태 변경이 반영되지 않으므로
            log.error("재고 판매 불가 처리 중 예외 발생 - productId: {}, reason: {}",
                    event.productId(), e.getMessage(), e);
            // TODO: 보상 이벤트 발행 - 예기치 않은 오류 발생 시 알림 및 재처리 필요
        }
    }
}
