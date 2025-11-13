# Rocket Delivery

---

### 프로젝트 소개

Rocket Delivery는 B2B 물류 서비스로 주문 접수부터 재고 예약, 허브 라우팅, 배송/기사 관리, Slack 기반 운영 알림까지 전 과정을 마이크로서비스로 분리한 시스템입니다. 
각 도메인은 DDD 레이어드 구조(Interface → Application → Domain → Infrastructure)를 따르며, Kafka 이벤트/내부 도메인 이벤트로 상호 통신합니다. 
공통 모듈(module-common, module-jpa, module-kafka)이 에러 전략, 베이스 엔티티, 메시징 설정을 제공하여 서비스 간 일관성을 유지합니다.

---

## 목차
- [프로젝트 소개](#프로젝트-소개)
- [핵심 기능](#핵심-기능)
- [아키텍처 다이어그램](#아키텍처-다이어그램)
- [기술 스택](#기술-스택)
- [팀원 소개](#팀원-소개)
- [코드 구조](#코드-구조)
- [테스트 코드](#테스트-코드)
- [실행 방법](#실행-방법)
- [라이선스](#라이선스)

---

### 핵심 기능

| 도메인 영역 | 관련 모듈 | 주요 책임 |
|-------------|-----------|-----------|
| 사용자 관리 | `module-user` | 역할(마스터/허브/배송/업체) 기반 권한, 승인형 회원가입(PENDING→APPROVED/REJECTED), JWT 로그인·토큰 발급, 본인/관리자 조회 정책, `deleted_at/by` 소프트 삭제 |
| 허브 & 경로 설계 | `module-hub` | 허브 CRUD·검색, 허브간 이동정보 모델링(P2P, Hub&Spoke 등), Redis 캐시, 논리 삭제 일관성, 마스터 전용 수정/삭제 권한 |
| 업체 관리 | `module-company` | 업체 CRUD·검색, 허브 유효성 검증, 담당 허브 권한 제어(마스터·허브 관리자·본인 업체), 소프트 삭제 연동 |
| 상품·재고 | `module-product` | 상품/카테고리 CRUD, 업체/허브 검증, 소프트 삭제, 재고 차감·복구, 2단계 재고 예약(예약→확정/취소), 낙관적 락 동시성 제어 |
| 주문·결제 오케스트레이션 | `module-order` | 주문 생성 시 배송/재고 이벤트 동시 처리, 결제/취소 흐름, 허브·업체 권한 검증, `ProcessedEvent` 중복 방지|
| 배송 & 배송담당자 | `module-delivery`, `module-deliverylog`, `module-deliveryman` | 주문 시 배송·경로·로그 자동 생성, 상태/거리/시간 추적, 배송담당자 순번 배정·CRUD, 역할별 접근 제어(허브/배송 담당자), 소프트 삭제 관리 |
| Slack 알림 & 메시지 관리 | `module-slack` | 주문 시 AI 계산 “최종 발송 시한” DM(발송 허브 담당자), 매일 06시 네이버 경로 기반 일일 루트 알림(업체 배송 담당자), Slack/메일 메시지 저장·API 발송·권한 정책 |
| 플랫폼 인프라 & Gateway | `module-gateway`, `module-server`, `module-common`, `module-jpa`, `module-kafka` | Spring Cloud Gateway 인증/라우팅, Eureka 서비스 디스커버리, 공통 예외·응답·도메인 이벤트, BaseEntity/JPA/Kafka 설정, Zipkin·Docker Compose 기반 MSA 인프라 |

### 아키텍처 다이어그램


![Rocket Delivery Architecture Placeholder](./docs/architecture-overview.png)

---

### 기술 스택

| 영역 | 구성 요소 |
|------|-----------|
| Language & Framework | Java 17, Spring Boot 3.5.6, Spring Cloud 2025.0.0, Spring Data JPA, Spring Security, Spring Cloud Gateway, Netflix Eureka |
| Messaging & Async | Apache Kafka, Spring for Kafka, Spring Events/`@TransactionalEventListener`, `@Async` 기반 비동기 처리 |
| Database & Cache | PostgreSQL 16, Redis 6.2, H2 (테스트), JPA + Query 기반 저장소 |
| Infra & Ops | Docker Compose, Zipkin(분산 트레이싱), Kafka UI, PgAdmin, Redis CLI |
| Build & QA | Gradle 8.14, JUnit 5, Mockito, AssertJ, RestAssured, Jacoco |

---

### 팀원 소개 
<div align="center">
  <table>
    <tbody>
      <tr>
        <td align="center" style="padding: 20px;">
          <img src="https://avatars.githubusercontent.com/u/205297434?v=4" width="120px" style="border-radius: 50%;" alt=""/>
          <div style="margin-top: 10px; font-size: 14px; line-height: 1.2;">
            <b>팀원</b><br />
            <a href="https://github.com/dain391" style="font-size: 16px;">김한빈</a>
            <div style="margin-top: 5px; font-size: 14px;">
            </div>
          </div>
        </td>
        <td align="center" style="padding: 20px;">
          <img src="https://avatars.githubusercontent.com/u/149287600?v=4" width="120px" style="border-radius: 50%;" alt=""/>
          <div style="margin-top: 10px; font-size: 14px; line-height: 1.2;">
            <b>팀원</b><br />
            <a href="https://github.com/Hojeong016" style="font-size: 16px;">이유진</a>
            <div style="margin-top: 5px; font-size: 14px;">
            </div>
          </div>
        </td>
        <td align="center" style="padding: 20px;">
          <img src="https://avatars.githubusercontent.com/u/172055043?v=4" width="120px" style="border-radius: 50%;" alt=""/>
          <div style="margin-top: 10px; font-size: 14px; line-height: 1.2;">
            <b>팀원</b><br />
            <a href="https://github.com/maengjjin" style="font-size: 16px;">박결</a>
            <div style="margin-top: 5px; font-size: 14px;">
            </div>
          </div>
        </td>
        <td align="center" style="padding: 20px;">
          <img src="https://avatars.githubusercontent.com/u/175666135?v=4?" width="120px" style="border-radius: 50%;" alt=""/>
          <div style="margin-top: 10px; font-size: 14px; line-height: 1.2;">
            <b>팀원</b><br />
            <a href="https://github.com/minsoo-hub" style="font-size: 16px;">채호정</a>
            <div style="margin-top: 5px; font-size: 14px;">
            </div>
          </div>
        </td>
      </tr>
    </tbody>
  </table>
</div>

---

## 코드 구조

### 모듈 구성

```
rocket-delivery/
├── module-gateway          # Spring Cloud Gateway 진입점 및 OpenAPI 집계
├── module-server           # Eureka Server (Service Discovery)
├── module-order            # 주문·결제 애그리게이트 및 이벤트 허브
├── module-product          # 상품/카테고리/재고 + 재고 예약 도메인
├── module-company          # 파트너사/입점사 관리
├── module-hub              # 허브/경로/라우트 계획 + Redis 캐시
├── module-delivery         # 배송 일정/상태 관리
├── module-deliveryman      # 라이더/파트너 기사 관리, 이벤트 발행
├── module-deliverylog      # 배송 경로/이벤트 로그 수집
├── module-user             # 계정, 권한, JWT 인증/인가
├── module-slack            # 운영 Slack/메일 알림 서비스
├── module-common           # 공통 예외, API 응답, 도메인 이벤트 베이스
├── module-jpa              # BaseEntity, JPA/Redis 공용 설정
├── module-kafka            # Kafka 공통 설정/직렬화
└── module-hub/kafka-init…  # 인프라 초기화 스크립트 (PostgreSQL, Kafka 등)
```

- 각 서비스는 `presentation → application → domain → infrastructure` 패키지 구조와 DTO/VO/Entity 분리를 지킵니다.
- 이벤트 중복 방지를 위해 주문·상품 모듈에 `ProcessedEvent` 테이블/저장소가 포함되어 있으며, Slack 모듈은 CQRS로 명령/조회 모델을 분리합니다.
- 공통 설정(`module-common`, `module-jpa`, `module-kafka`)은 Gradle 멀티모듈 의존성으로 재사용됩니다.

---

## 테스트 코드

테스트 정책은 `TEST_GUIDE.md`에 정의된 네이밍/Fixture 규칙을 따릅니다. 
모든 모듈은 `src/test/java`에 미러 구조를 가지며 `application-test.yml`에서 H2 인메모리 DB, 비활성화된 Eureka/Kafka 설정으로 실행됩니다.


- 전 모듈 공통 실행: `./gradlew test`
- 모듈별 실행:

| 모듈 | 명령어 |
|------|--------|
| module-order | `./gradlew :module-order:test` |
| module-product | `./gradlew :module-product:test` |
| module-delivery | `./gradlew :module-delivery:test` |
| module-deliveryman | `./gradlew :module-deliveryman:test` |
| module-deliverylog | `./gradlew :module-deliverylog:test` |
| module-hub | `./gradlew :module-hub:test` |
| module-company | `./gradlew :module-company:test` |
| module-user | `./gradlew :module-user:test` |
| module-slack | `./gradlew :module-slack:test` |
| module-gateway / module-server 등 공통 모듈 | `./gradlew :module-gateway:test`, `./gradlew :module-server:test`, `./gradlew :module-common:test`, … |

- 테스트 리포트: `build/reports/tests/test/index.html`


---

## 실행 방법

### 사전 요구사항
- Java 17+
- Docker & Docker Compose
- (선택) PgAdmin, Redis CLI, Kafka UI 사용 시 로컬 포트 충돌 없음

### 인프라 실행

```bash
# PostgreSQL, Redis, Kafka, Zipkin 등 필수 인프라 기동
docker-compose up -d
```

### 서비스 포트

| 서비스 | 포트 |
|--------|------|
| PostgreSQL | 5433 |
| PgAdmin | 5050 |
| Redis | 6378 |
| Kafka | 9092 |
| Kafka UI | 9999 |
| Zipkin | 9411 |

### 빌드 & 실행

```bash
# 전체 빌드
./gradlew clean build

# 개별 모듈 빌드/테스트
./gradlew :module-product:build
./gradlew :module-order:test

# 서비스 기동 (필요 모듈 조합)
./gradlew :module-server:bootRun          # Eureka
./gradlew :module-gateway:bootRun         # API Gateway
./gradlew :module-user:bootRun            # 인증 서비스
./gradlew :module-product:bootRun         # 상품/재고
./gradlew :module-order:bootRun           # 주문
./gradlew :module-delivery:bootRun        # 배송
./gradlew :module-slack:bootRun           # 알림
```

> `bootRun`은 기본적으로 `local` 프로파일을 사용하며, 필요 시 `./gradlew :module-order:bootRun -Dspring.profiles.active=dev` 형태로 지정합니다.

---
