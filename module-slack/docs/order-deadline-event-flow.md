# 주문 완료 → Slack 발송 시한 이벤트 설계

주문 서비스에서 주문이 최종 확정될 때 REST 호출 대신 **Spring Application Event** 를 발행하면 Slack 모듈이 같은 애플리케이션 컨텍스트 안에서 이벤트를 전파받아 발송 시한 알림을 실행할 수 있습니다. 아래는 `module-common`의 `EventPublisher`(도메인 이벤트 래퍼)를 활용한 설계 초안입니다.

---

## 1. 도메인 이벤트 정의

| 항목 | 내용 |
| --- | --- |
| 이벤트 명 | `OrderDeadlineRequestedEvent` (예시) |
| 위치 | `module-order` (주문 BC) |
| 인터페이스 | `org.sparta.common.event.DomainEvent` |
| 필수 필드 | `eventId`, `occurredAt`, `orderId`, `orderNumber`, `customer`, `originHubId`, `originHubName`, `destinationCompanyId`, `destinationAddress`, `transitPath`, `deliveryDeadline`, `workHours`, `quantity`, `productInfo`, `memo`, `deliveryManagerName/email/slackId` 등 |

```java
public record OrderDeadlineRequestedEvent(
        UUID eventId,
        Instant occurredAt,
        Payload payload
) implements DomainEvent {

    public record Payload(
            UUID orderId,
            String orderNumber,
            UUID originHubId,
            String originHubName,
            String originAddress,
            UUID destinationCompanyId,
            String destinationCompanyName,
            String destinationAddress,
            String transitPath,
            LocalDateTime deliveryDeadline,
            Integer workStartHour,
            Integer workEndHour,
            Integer quantity,
            String productInfo,
            String requestMemo,
            String customerName,
            String customerEmail,
            String deliveryManagerName,
            String deliveryManagerEmail
    ) {}
}
```

- `eventId`: 주문 Aggregate의 ID 또는 별도 UUID (멱등성 보장).
- `occurredAt`: 주문 완료 시각 (UTC Instant).
- `payload`: Slack 모듈에서 `OrderDeadlineCommand`를 만들 수 있을 정도로 필요한 필드만 포함.

---

## 2. 주문 모듈에서 이벤트 발행

1. 주문 상태가 **배송 준비 완료**(또는 “최종 확정”)로 전환되는 도메인 서비스/애플리케이션 서비스에서 EventPublisher 주입.
2. 주문·허브·근무시간·상품 정보를 `payload`에 채워 `OrderDeadlineRequestedEvent` 생성.
3. `eventPublisher.publishLocal(event)` 호출 → 동일한 Spring Context에 등록된 Listener에게 전달.

```java
@Service
@RequiredArgsConstructor
public class OrderCompletionService {

    private final EventPublisher eventPublisher;

    @Transactional
    public void complete(Order order) {
        // 주문 확정 로직 …
        order.markCompleted();

        OrderDeadlineRequestedEvent event = OrderDeadlineRequestedEventMapper.from(order);
        eventPublisher.publishLocal(event);
    }
}
```

> **Kafka 연동 여지**: 향후 다른 Bounded Context에서도 이 이벤트를 읽어야 한다면 `publishExternal(event)` 도 함께 호출해 `order-events` 토픽으로 전달할 수 있습니다.

---

## 3. Slack 모듈 Listener 설계

| 컴포넌트 | 역할 |
| --- | --- |
| `OrderDeadlineEventListener` (`module-slack`) | `@EventListener` 또는 `@TransactionalEventListener` 로 주문 이벤트 수신 |
| `OrderDeadlineFacade` | 기존 REST 흐름과 동일하게 AI 계산 → Slack DM 실행 |
| `OrderDeadlineCommandMapper` | 이벤트 payload → `OrderDeadlineCommand` 변환 전담 |

```java
@Component
@RequiredArgsConstructor
public class OrderDeadlineEventListener {

    private final OrderDeadlineFacade orderDeadlineFacade;

    @TransactionalEventListener
    public void handle(OrderDeadlineRequestedEvent event) {
        if (event == null || event.payload() == null) {
            return;
        }
        OrderDeadlineCommand command = OrderDeadlineCommandMapper.from(event.payload());
        orderDeadlineFacade.notify(command);
    }
}
```

- `@TransactionalEventListener(phase = AFTER_COMMIT)`을 사용하면 주문 트랜잭션이 커밋된 이후에만 Slack 알림을 트리거할 수 있습니다.
- 동일한 Facade/Service/Template/테스트를 재사용하므로 REST 엔드포인트는 **백업 경로**로 유지하거나 필요 시 제거할 수 있습니다.

---

## 4. 멱등성 및 에러 처리

| 주제 | 제안 |
| --- | --- |
| 이벤트 중복 | `eventId` 또는 `orderId`를 기준으로 Slack 측에서 이미 발송된 메시지인지 체크(`MessageRepository` / 캐시). 필요 시 `OrderDeadlineCommand`에 `requestId` 추가. |
| 예외 처리 | Listener에서 예외 발생 시 `@TransactionalEventListener`는 롤백되지 않으므로 `OrderDeadlineFacade.notify()` 내부 try-catch 로깅 유지. 재처리가 필요하면 Dead Letter 큐나 Retry 이벤트 발행 고려. |
| 장애 대응 | 이벤트 처리 실패 시 재시도/보류 큐를 두거나, 별도 fallback API를 추가해 주문 서비스가 재요청할 수 있는 전략 마련. |

---

## 5. 구성/설정

1. `app.eventpublisher.enabled=true` (default)인지 확인.
2. 주문 서비스의 Spring Context에 `EventPublisher` 빈이 이미 존재하므로 별도 설정 불필요.
3. Slack 모듈에서 Listener가 동작하려면 동일한 애플리케이션(모놀리식) 내에서 실행되거나, 만약 모듈별 별도 서비스라면 Kafka 같은 외부 브로커로 전환해야 합니다.

---

## 6. 단계별 Migration 가이드

| 단계 | 설명 |
| --- | --- |
| 1 | `OrderDeadlineRequestedEvent` 추가 및 주문 서비스에서 발행 |
| 2 | Slack 모듈에 `OrderDeadlineEventListener`, Mapper 추가 |
| 3 | 통합 테스트: 주문 서비스에서 이벤트 발행 → Slack 모듈 Facade 호출까지 검증 |
| 4 | 운영 중 REST 엔드포인트 사용 여부 확인 후, 이벤트 전환이 안정화되면 REST 호출 제거 또는 보조 경로로 유지 |

---

## 7. 확장 포인트

- **Kafka 기반 다중 BC**: `EventPublisher.publishExternal`을 함께 호출해 다른 마이크로서비스도 동일 이벤트를 소비.
- **Saga/Retry**: 이벤트 처리 실패 시 별도 “OrderDeadlineFailedEvent”를 발행해 주문 서비스가 재시도 로직을 수행할 수 있도록 설계.
- **메트릭/모니터링**: 이벤트 수신 성공/실패 건수를 Prometheus Metric 으로 노출.

이와 같은 이벤트 드리븐 구조로 전환하면 주문 서비스는 Slack 모듈과의 직접 REST 호출에 의존하지 않고, 트랜잭션 완료 후 비동기적으로 알림을 트리거할 수 있습니다. Slack 모듈은 기존 Facade/Service/템플릿을 그대로 활용하므로 구현 범위는 Listener + Mapper 수준에 그칩니다.
