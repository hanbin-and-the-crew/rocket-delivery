# Rocket Delivery

---

## 목차

1. [Step 회고](#step-회고)
   - [Step1](#step1)
   - [Step2](#step2)
   - [Step3](#step3)
   - [Step4](#step4)
   - [Step5](#step5)
2. [Jacoco 리포트](#jacoco-리포트)
3. [아키텍처](#아키텍처)
   - [모듈 구조](#모듈-구조)
4. [시작하기](#시작하기)
   - [사전 요구사항](#사전-요구사항)
   - [인프라 실행](#인프라-실행)
   - [서비스 포트](#서비스-포트)
   - [빌드 & 실행](#빌드--실행)


---

## TEST_GUIDE 문서

[TEST_GUIDE.md](https://github.com/hanbin-and-the-crew/rocket-delivery/blob/tdd_main/TEST_GUIDE.md)

---

## Step 회고 
토글을 클릭하면 각 Step(1~5)의 회고 내용을 도메인별로 확인하실 수 있습니다.


### Step1

<details>
  <summary>User</summary>


</details>

<details>
  <summary>Order</summary>


</details>

<details>
  <summary>Hub</summary>


</details>

<details>
  <summary>Product</summary>


</details>

---

### Step2

<details>
  <summary>User</summary>


</details>

<details>
  <summary>Order</summary>


</details>

<details>
  <summary>Hub</summary>


</details>

<details>
  <summary>Product</summary>


</details>

----

### Step3

<details>
  <summary>User</summary>


</details>

<details>
  <summary>Order</summary>


</details>

<details>
  <summary>Hub</summary>


</details>

<details>
  <summary>Product</summary>


</details>


----

### Step4

<details>
  <summary>User</summary>


</details>

<details>
  <summary>Order</summary>


</details>

<details>
  <summary>Hub</summary>


</details>

<details>
  <summary>Product</summary>


</details>


---

### Step5

<details>
  <summary>User</summary>


</details>

<details>
  <summary>Order</summary>


</details>

<details>
  <summary>Hub</summary>


</details>

<details>
  <summary>Product</summary>


</details>

---

## Jacoco 리포트

### User

```
라인 커버리지: %
브랜치 커버리지: %
```

### Order

```
라인 커버리지: %
브랜치 커버리지: %
```

### Hub

```
라인 커버리지: %
브랜치 커버리지: %
```

### Product

<img width="1180" height="345" alt="Image" src="https://github.com/user-attachments/assets/2a15eca2-8494-4695-967f-7fb2aaf516ec" />

```
라인 커버리지: %
브랜치 커버리지: %
```


---



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
