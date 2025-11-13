# 주문 발송 시한 Slack 알림 구현 계획

## 1. 목표
- 주문이 접수되는 즉시 발송 허브 담당자에게 **최종 발송 시한**을 Slack DM으로 안내한다.
- “AI 계산 → Slack 템플릿 렌더링 → 메시지 저장/전송” 흐름을 Notification BC 안에서 완결한다.
- 기존 일일 경로 알림과 동일하게 레이어드 아키텍처(Application Service → Domain/Port)를 지킨다.

## 2. 이벤트 흐름
```
Order Service
   └─ OrderDeadlineEventListener
         └─ OrderDeadlineFacade
        ├─ OrderDeadlinePlanningService  (Gemini 호출, 발송 시한 계산)
        └─ OrderSlackNotificationService (기존 Slack 저장/발송 재사용)
```

## 3. 컴포넌트 설계
| 계층 | 컴포넌트 | 역할 |
| --- | --- | --- |
| Application | `OrderDeadlineEventListener` | 주문 완료 도메인 이벤트를 수신해 Facade 호출 |
| Application | `OrderDeadlineFacade` | 발송 시한 계산 → Slack 알림까지 조율 |
| Application | `OrderDeadlinePlanningService` | Gemini 프롬프트 구성, 최종 발송 시한 계산, Fallback |
| Application | `OrderSlackNotificationService` | 이미 존재. 템플릿 새로 추가 (예: `ORDER_DEADLINE_ALERT`) |
| Config | `OrderDeadlineTemplateInitializer` (기존 파일 확장 or 신규) | 템플릿 시딩/업데이트 |
| Domain/Ports | 기존 `SlackRecipientFinder`, `SlackTemplateRepository`, `MessageRepository` 재사용 |

## 4. 구현 단계
1. **이벤트 / Command 매핑**
   - 주문 서비스에서 발행하는 `OrderDeadlineRequestedEvent` payload를 Slack 모듈에서 수신.
   - `OrderDeadlineEventListener` + `OrderDeadlineCommandMapper`로 이벤트 → `OrderDeadlineCommand` 변환.
   - 테스트: 매퍼/리스너 단위 테스트로 필드 매핑 및 Facade 호출 검증.

2. **발송 시한 계산 서비스**
   - `OrderDeadlinePlanningService` 작성.
   - 입력: 요청 DTO → 내부 Command (ex. `OrderDeadlineCommand`).
   - 로직: Gemini RestClient 호출 → JSON 파싱 → `OrderDeadlineResult`(deadline, aiReason).
   - Fallback: 근무시간/납기 기반 간단 계산.
   - 테스트: `RestClient` mock으로 정상/실패 케이스 검증.

3. **Facade & Slack 전송**
   - `OrderDeadlineFacade`가 Planning 서비스와 기존 `OrderSlackNotificationService`를 조합.
   - Slack 템플릿 코드 `ORDER_DEADLINE_ALERT` 사용. payload에는 주문 요약 + deadline 포함.
   - 템플릿 시딩 로직(`RouteSummaryTemplateInitializer` → `NotificationTemplateInitializer` 등) 확장.
   - 테스트: Facade 단위 테스트 (planning 결과 & Slack 서비스 mock).

4. **권한/수신자 정책**
   - 허브 담당자 조회: `SlackRecipientFinder#findApprovedByHubAndRoles`.
   - 컨트롤러에서 hubId·roles 입력을 검증하거나 기본값(DELIVERY_MANAGER) 사용.
   - 필요 시 `OrderSlackNotificationRequest` 확장.

5. **문서/테스트 업데이트**
   - `slack-dispatch-summary.md`에 주문 발송 시한 플로우 추가.
   - 이벤트 흐름 다이어그램 및 통합 테스트 시나리오 정리.
   - 통합 테스트: Facade + Mock RestClient + InMemory Template? (선택)

## 5. 테스트 전략
| 단계 | 테스트 |
| --- | --- |
| Event Listener | `OrderDeadlineEventListenerTest` – 이벤트 수신 시 Facade 호출 |
| Mapper | `OrderDeadlineCommandMapperTest` – payload 매핑 검증 |
| Planning | `OrderDeadlinePlanningServiceTest` – 정상응답, Gemini 실패 fallback |
| Facade | `OrderDeadlineFacadeTest` – planning + Slack notify 조합, 예외 시 처리 |
| Template | `NotificationTemplateInitializerTest` – `ORDER_DEADLINE_ALERT` 존재/업데이트 검증 |

## 6. 향후 고려사항
- 주문 서비스에서 직접 호출하는 경우를 대비해 Kafka/이벤트 리스너 추가 가능.
- Planning 결과를 `PlanningAggregate`로 확장할 때 Facade 로직 재사용 가능하도록 Command/Result VO를 명확히 유지.
