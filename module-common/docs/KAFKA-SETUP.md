# Kafka & Event 설정 가이드

## 🚀 빠른 시작

### 1. Kafka 실행

```bash
# Docker Compose로 Kafka 실행
docker-compose up -d kafka

# Kafka UI 접속
http://localhost:9999
```

---

## 모듈별 설정

---

## ⚙️ 설정 방법

### Step 1: build.gradle

```gradle
// Kafka 사용하는 모듈 (user, slack, product, delivery 등)
dependencies {
    implementation project(':module-common')
    implementation project(':module-jpa')
    implementation project(':module-kafka')  
}
```

### Step 2: application.yml

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: user-service  # 모듈명-service로 변경
```

**group-id 예시:**
- user 모듈: `user-service`
- slack 모듈: `slack-service`
- product 모듈: `product-service`

---

##  EventPublisher 사용

### 3가지 메서드

```java
@Service
@RequiredArgsConstructor
public class YourService {
    private final EventPublisher eventPublisher;

    // 1. 같은 모듈 내부만
    eventPublisher.publishLocal(event);

    // 2. 다른 모듈로 전파 (Kafka)
    eventPublisher.publishExternal(event);
    
}
```

---

##  Kafka 토픽 자동 결정

이벤트 클래스 이름에 따라 자동으로 토픽이 결정됩니다:

| 이벤트 클래스 | Kafka 토픽 |
|-------------|-----------|
| `UserCreatedEvent` | `user-events` |
| `OrderCreatedEvent` | `order-events` |
| `PlanningCompletedEvent` | `planning-events` |
| `MessageSentEvent` | `message-events` |
| 기타 | `domain-events` |

---

## 테스트

```bash
# 1. Kafka 실행 확인
docker ps | grep kafka

# 2. Kafka UI 접속
open http://localhost:9999

# 3. 토픽 확인
# Kafka UI에서 Topics 탭 확인
```

---

##  문제 해결

### KafkaTemplate을 찾을 수 없음
→ `build.gradle`에 `module-kafka` 추가

### 메시지가 전송되지 않음
→ `docker-compose up -d kafka` 실행 확인
→ `application.yml`의 `bootstrap-servers` 확인

### Consumer가 메시지를 못 받음
→ `group-id`가 올바른지 확인
→ Kafka UI에서 토픽 생성 확인

---

**Kafka UI**: http://localhost:9999
**Kafka Port**: 9092