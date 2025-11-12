# User → Slack 이벤트 발행 가이드

Slack 컨텍스트는 `user-events` Kafka 토픽을 구독하여 `UserSlackView` 테이블을 최신 상태로 유지합니다. 아래 내용은 User 도메인 담당자가 어떤 시점에 어떤 이벤트를 발행해야 Slack 모듈이 정상 동작하는지를 설명합니다.

## 1. 이벤트 공통 규약

- **토픽**: `user-events` (EventPublisher가 `User*Event` 클래스명을 기반으로 자동 매핑)
- **이벤트 타입**: `User` 로 시작하는 DomainEvent 구현체 (예: `UserCreatedEvent`)
- **필수 필드 (payload)**

  | 필드 | 타입 | 비고 |
    | --- | --- | --- |
  | `userId` | `UUID` | User PK |
  | `userName` | `String` | 로그인 ID |
  | `realName` | `String` | 실명 |
  | `slackId` | `String` | Slack 사용자 ID |
  | `role` | `UserRoleEnum` | `MASTER/HUB_MANAGER/DELIVERY_MANAGER/COMPANY_MANAGER` |
  | `status` | `UserStatusEnum` | `PENDING/APPROVE/REJECTED` |
  | `hubId` | `UUID` | 소속 허브 |

Slack 모듈은 동일 이벤트를 여러 번 받아도 `occuredAt`/`lastEventAt`으로 멱등 처리하므로, 이벤트 재발행을 허용합니다.

## 2. 발행 시점

| 이벤트 타입 | 발행 시점 | Slack 모듈 처리 |
| --- | --- | --- |
| `UserCreatedEvent` | 사용자 생성 직후 | 신규 행 저장 |
| `UserUpdatedEvent` | 실명/이메일/Slack ID 등 프로필 변경 | 기존 행 업데이트 |
| `UserRoleChangedEvent` | 역할 변경 승인 | 역할/권한 업데이트 |
| `UserSlackUpdatedEvent` | Slack ID만 단독 변경 | Slack ID 갱신 |
| `UserDeletedEvent` | 사용자 삭제/비활성화 | `deletedAt` 설정 (Soft Delete) |

> **Tip**: 프로필·역할·Slack ID 변경을 별도 이벤트로 나눴더라도 payload 스키마는 동일하게 유지하면 Slack 모듈이 단일 DTO(`UserDomainEvent`)만으로 처리할 수 있습니다.

## 3. 구현 가이드

1. **도메인 이벤트 정의**
   ```java
   public record UserCreatedEvent(
       UUID eventId,
       Instant occurredAt,
       UserPayload payload
   ) implements DomainEvent {
       public static UserCreatedEvent of(User user) {
           return new UserCreatedEvent(
               UUID.randomUUID(),
               Instant.now(),
               UserPayload.from(user)
           );
       }
   }
   ```
2. **EventPublisher 사용**  
   User 서비스의 Application Service에서 사용자 상태가 변경되는 트랜잭션이 완료되면 `eventPublisher.publishExternal(event)` 호출.
3. **Idempotency**  
   동일 이벤트가 중복 발행돼도 Slack 모듈은 `eventTime`이 최신인 경우에만 반영하므로 추가 작업은 필요 없음.

## 4. 주의 사항

- **널 값 방지**: Slack 모듈은 payload 누락 시 `USER_SLACK_VIEW_PAYLOAD_MISSING` 예외를 발생시키므로, 필수 필드가 비어 있지 않도록 검증 후 발행합니다.
- **Slack ID 변경 시점**: Slack ID 입력이 필수이므로, 가입 시점에 반드시 Slack ID를 확보하거나 Stub 값을 두고 추후 `UserSlackUpdatedEvent`를 발행하세요.
- **토픽 모니터링**: Kafka UI(`http://localhost:9999`)에서 `user-events` 토픽을 선택하면 메시지 흐름을 확인할 수 있습니다.

이 가이드를 따라 User 서비스가 이벤트를 발행하면 Slack 모듈은 추가 의존 없이 사용자 Slack 정보를 최신 상태로 유지할 수 있습니다.
