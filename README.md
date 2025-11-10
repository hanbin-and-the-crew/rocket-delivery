# Rocket Delivery
---

---

## 프로젝트 개요

---

## 기술 스택

### Backend

- 

## 아키텍처

### 모듈 구조

```
rocket-delivery/
├── module-gateway          # API Gateway
├── module-server           # Eureka Server (Service Discovery)
├── module-product          # 상품 관리 서비스
├── module-company          # 업체 관리 서비스
├── module-hub              # 허브 관리 서비스
├── module-order            # 주문 관리 서비스
├── module-user             # 사용자 관리 서비스
├── module-delivery         # 배송 관리 서비스
├── module-slack            # Slack 연동 서비스
├── module-common           # 공통 모듈
└── module-jpa              # JPA 공통 설정
```
---

## 시작하기

### 사전 요구사항

- Java 17 이상
- Docker & Docker Compose

### 인프라 실행

```bash
# Docker 컨테이너 실행 (PostgreSQL, Redis, Kafka, Zipkin 등)
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

# 특정 모듈 빌드
./gradlew :module-product:build

# 애플리케이션 실행
./gradlew :module-gateway:bootRun
./gradlew :module-server:bootRun
./gradlew :module-product:bootRun
```