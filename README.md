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

## STEP
---

Step 1

## 각 Step별 학습 내용과 느낀점

---

- 학습 내용:
  - User 도메인의 핵심 로직(생성, 상태 변경, 필드 수정)을 중심으로 도메인 단위 테스트를 작성했다. 
  - Given-When-Then 구조를 명확히 구분하여 테스트 의도를 표현하고, 예외 상황(BusinessException)을 검증했다. 
- 느낀 점:
  - 처음에는 단순한 테스트처럼 보였지만, 생성자 검증과 상태 전이 로직을 명확히 정의하는 과정에서 도메인 요구사항이 더 구체화되었다. 
  - 유효성 검사를 코드로 명시함으로써 안전한 도메인 모델링의 중요성을 체감했다. 


## TDD 학습 과정 정리

---

- Red 단계:
  - User 생성 시 유효성 검사를 수행하지 않아 예외가 발생하지 않는 문제를 발견했다.
- Green 단계:
  - User.create() 내부에 username, email, slackId 검증 로직을 추가하고, 테스트를 통과시켰다.
- Refactor 단계:
  - 중복되는 생성 코드들을 UserFixture로 추출하여 재사용성을 높였다. 
  - 이후 updateStatus() 경계 테스트를 통해 로직 누락을 점검했다.


## 테스트 전략 설명

---

Step1: 단위 테스트(Unit Test)
- User 엔티티의 생성 및 상태 변경 로직이 명세대로 작동하는지 검증
- 정상 입력과 예외 입력 케이스 분리 
- 파라미터라이즈드 테스트로 상태 전이 경계값 검증 
- Fixture를 활용한 중복 제거



## 도메인 요구사항

---

- 회원 생성 시 필수값 검증
  - username, email, slackId가 비어 있으면 BusinessException 발생
- 회원 기본 상태는 PENDING으로 설정
- 회원 상태 변경 제약
  - PENDING 상태에서만 APPROVE 또는 REJECTED로 변경 가능
  - 이미 APPROVE 또는 REJECTED 상태인 회원은 다시 상태를 변경할 수 없음
- 회원 정보 수정 기능 제공
  - 비밀번호, 이메일, 전화번호 수정 가능
- 예외 메시지 명시화
  - 예: "username은 필수입니다.", "대기중인 회원만 상태를 변경할 수 있습니다."


## 테스트 커버리지
```
User Entity 단위 테스트의 jacoco 예시입니다.
라인 커버리지: 78%
브랜치 커버리지: 70%
```
---
아래처럼 User 모듈에서 원하는 테스트만 돌렸다.
```
./gradlew :module-user:test --tests "org.sparta.user.domain.entity.UserTest" :module-user:jacocoTestReport

start module-user/build/reports/jacoco/test/html/index.html
```
## 어려웠던 점 / 개선하고 싶은 부분(회고)

---

어떠한 방식으로 테스트 코드와 TDD 방식이 진행되는지는 알겠으나 이를 Jacoco 기준 100 퍼센트 적용하기에는 어렵다고 느껴졌다.

특히 금융이나 결제 쪽 도메인은 테스트 코드가 매우 까다로울텐데 모든 경우의 수를 다 고려해야만겠다는 생각이 들었던 것 같다.

아쉬운 점은 테스트 코드를 적는 시간보다 환경 설정 특히, JWT 쪽 문제로 인하여 온전히 User 테스트 코드 작성에 많은 시간을 사용하지 못하였다.

Bean, env, Config, Jwt 인증/인가 부분이 테스트 코드에서 어떻게 얽혀져 있는지에 대한 개념이 부족함을 느꼈다.

더욱 이론적인 공부가 필요하다고 스스로 느낀다.


## 체크리스트
## Step 1:도메인 모델 테스트

### **구현 체크리스트:**

- [x]  도메인 모델 클래스와 테스트 코드 작성 (5개 이상 테스트 케이스)
- [x]  각 테스트에 Given-When-Then 구조 명시
- [x]  TDD 사이클을 경험한 느낀 점을 README에 간단히 


</details>

<details>
  <summary>Order</summary>


</details>

<details>
  <summary>Hub</summary>

## Hub 도메인 모델 테스트

### 구현 내용

- Hub 엔티티의 생성, 상태 변경(`ACTIVE` ↔ `INACTIVE`), 삭제(`markAsDeleted()`) 로직을 테스트 기반으로 작성
- 상태 변경은 실제 삭제 대신 `INACTIVE` 처리하여 데이터 이력 유지.

### 테스트 예시

```java
class HubTest {

    @Test
    @DisplayName("허브 생성 시 기본 상태는 ACTIVE다")
    void createHub_defaultStatusIsActive() {
        Hub hub = Hub.builder()
            .hubName("서울 허브")
            .city("서울특별시")
            .build();

        assertThat(hub.getStatus()).isEqualTo(HubStatus.ACTIVE);
    }

    @Test
    @DisplayName("허브 삭제 시 상태가 INACTIVE로 변경된다")
    void markAsDeleted_changesStatusToInactive() {
        Hub hub = Hub.builder().hubName("테스트 허브").city("서울").build();

        hub.markAsDeleted();

        assertThat(hub.getStatus()).isEqualTo(HubStatus.INACTIVE);
        assertThat(hub.getDeletedAt()).isNotNull();
    }
}
```

### 체크리스트

- [x]  Entity 클래스 테스트 5개 이상 작성
- [x]  Given-When-Then 구조 유지
- [x]  상태 전환 및 예외 케이스 검증

### 회고

Hub 엔티티의 생성과 상태 전환(ACTIVE ↔ INACTIVE)을 테스트 기반으로 구현했다.
markAsDeleted()로 실제 삭제 없이 INACTIVE 처리하는 과정을 통해 Soft Delete 개념을 자연스럽게 이해했다.
테스트 작성이 오히려 “도메인 규칙을 정의하는 행위”라는 걸 체감할 수 있었다.

</details>

<details>
  <summary>Product</summary>

## Step 1


## 각 Step별 학습 내용과 느낀 점

### 학습 내용

#### 도메인 모델의 책임 범위 이해
- 도메인 모델은 **순수한 비즈니스 로직만 담당**해야 함  
- “주문이 들어오면”, “결제가 완료되면”과 같은 **외부 트리거는 서비스 레이어의 책임**
- 예시  
  - `Product.create()` → 도메인 모델의 역할  
  -  “주문이 들어오면 상품을 생성한다” → 서비스의 역할  

#### 테스트 중심 개발 경험
- 테스트를 먼저 작성하니 **요구사항을 명확히 정리할 수 있었음**
- **주석 기반 테스트 케이스 설계**를 통해 **우선순위 중심 개발**이 가능해짐  
- **Given-When-Then 구조**로 테스트의 의도와 맥락이 명확해짐

### 느낀 점
- 처음에는 “주문이 들어오면 예약 상태 재고, 출고 시 차감”이 도메인 모델 범위라 생각했지만,  
  이는 **외부 트리거에 의한 로직**으로 서비스 레이어의 책임임을 이해함
- 테스트를 먼저 작성하니 **기획서 요구사항을 우선순위별로 정리하는 데 도움이 되었음**
- **Given-When-Then 구조** 생각하니 오히려 복잡하게 접근하기 않게 됨을 느꼈음
- **Mock 없이 순수 도메인 로직만 테스트**하니 테스트 초기 설정 및 작성에 리소스가 다른 테스트에 비해 적다고 느껴짐

---

## TDD 학습 과정 정리

### 요구사항 분석

#### Product 생성 시 검증 항목
- 상품명 필수
- 가격 필수 및 0원 이상
- 카테고리 ID 필수
- 업체 ID 필수 (존재 여부 검증)
- 허브 ID 필수 (존재 여부 검증)
- 재고 0 이상
- 상품 생성 시 Stock 자동 생성
- 논리적 삭제 기능 포함

---

## 테스트 전략 설명

### 단위 테스트 (Unit Test)
- **대상:** `Product`, `Stock` 도메인 엔티티  
- **목적:** 순수 비즈니스 로직 검증  
- **방식:** Mock 사용 없이 실제 객체 테스트  

### 테스트 구조

ProductTest (총 17개)
├── 정상 케이스 (4)
│ ├─ 유효한 입력으로 생성 성공
│ ├─ 재고 자동 생성 및 0 정상 처리
│ └─ Stock ID 일관성 검증
│
├── 예외 케이스 (8)
│ ├─ 상품명, 가격, 카테고리, 업체, 허브 ID 검증
│ └─ 재고량 검증 (null, 음수) - @ParameterizedTest 활용
│
└── 논리적 삭제 (3)
├─ 삭제 시 deletedAt 설정
├─ 중복 삭제 시 무효 처리
└─ 복원 시 deletedAt null 확인


---

## 도메인 요구사항

| 구분 | 검증 항목 | 테스트 |
|------|------------|--------|
| 업체 검증 | 존재 여부 확인 | 예외 발생 |
| 허브 ID | null 불가, Stock과 일치 | 일관성 검증 |
| 재고 생성 | Product 생성 시 자동 생성 | 자동 생성 확인 |
| 재고량 | 0 이상, 음수/Null 예외 | 검증 완료 |
| 삭제 처리 | Cascade로 Stock 삭제 | 통합 테스트 예정 |
| 가격 | 0원 이상, Null 예외 | 검증 완료 |

---

## 테스트 커버리지

<img width="1055" height="186" alt="Image" src="https://github.com/user-attachments/assets/c3258e4e-f21b-45d5-a306-b9afbcc2fc76" />

**Missed Branches(미커버 분기): 약 79%**

---

## 어려웠던 점 / 개선하고 싶은 부분

### 어려웠던 점
- 재고 부족, 중복 삭제 등 일부 시나리오가  
  **도메인 로직인지 서비스 로직인지 구분하기 어려웠음**
- 결과적으로 도메인에서는 **불변 조건(가격 ≥ 0, 재고 ≥ 0, 중복 삭제 방지)** 중심으로 예외를 처리하고,  
  **외부 트리거에 의한 상태 변화는 서비스 계층에서 처리**해야 함을 명확히 구분함

### 개선하고 싶은 부분
- **Missed Branches(분기 미커버)** 비율이 79%로 일부 조건문 분기 테스트가 누락됨  
- **가격 경계값(0원), 삭제된 상품 복원 등 경계 테스트 추가 필요**
- 재고/삭제 관련 조건문 분기 보강으로 **테스트 완성도 향상 목표**

---

## 체크리스트 

### Step 1: 도메인 모델 테스트
- [x] 도메인 모델 클래스와 테스트 코드 작성 (5개 이상 테스트 케이스)  
- [x] 각 테스트에 **Given-When-Then 구조 명시**  
- [x] **TDD 사이클**을 경험한 느낀 점을 README에 기록  

</details>

---

### Step2

<details>
  <summary>User</summary>

## STEP

---

Step 2

## 각 Step별 학습 내용과 느낀 점

---

- 학습 내용:
  - User 도메인 테스트(Step1)에서 확장하여 UserService의 비즈니스 로직을 단위 테스트했다. 
  - 외부 의존성을 가진 Repository, PasswordEncoder, SecurityService를 Mockito의 @Mock으로 대체하여 독립적인 테스트 환경을 구성했다. 
  - 회원가입, 조회, 탈퇴, 상태 변경 등의 핵심 기능을 시나리오 단위로 검증하고, 정상 케이스와 예외 케이스를 모두 포함했다. 
  - Mock 기반 테스트를 통해 실제 DB나 보안 로직에 의존하지 않고도 서비스 계층의 흐름을 명확히 검증할 수 있었다. 
- 느낀 점:
  - 처음엔 Mock 객체 설정이 다소 복잡했지만, given-when-then 구조를 통해 테스트 의도를 분리하니 가독성이 높아졌다. 
  - 실제 서비스 코드에서 Repository나 Encoder가 호출되는 시점을 명확히 파악할 수 있어, 비즈니스 로직 설계 검증에 큰 도움이 되었다. 
  - 단순한 단위 테스트를 넘어 예외 처리와 흐름 제어까지 검증하는 중요성을 체감했다.

## TDD 학습 과정 정리

---

- Red 단계:
  - UserService.signup()에서 중복 사용자 검증 로직이 없어서 테스트 실패 
  - 탈퇴 요청 시 예외 처리가 누락되어 BusinessException 미발생 
- Green 단계:
  - userRepository.findByUserName() 및 softDeleteByUserId()의 반환값에 따라 예외를 발생시키는 로직 추가 
  - 승인/거절 처리 시 UserStatusEnum.PENDING 상태만 변경 가능하도록 수정 
- Refactor 단계:
  - 중복되는 Mock 설정을 정리하고 verify()로 호출 여부를 명시적으로 검증 
  - 테스트 명세서 역할을 하도록 각 테스트에 명확한 @DisplayName을 추가

## 테스트 전략 설명

---

Step2: 단위 테스트 (Unit Test)
- UserService의 메서드를 독립적으로 테스트하며 외부 의존성을 Mock 처리
- 정상 케이스 + 예외 케이스 각각 검증
- Mock을 통한 Repository, Encoder 호출 여부와 인자값 확인
- 비즈니스 규칙(회원가입 중복, 탈퇴 예외, 상태 전이)을 명시적으로 검증


## 도메인 요구사항

---

- 회원가입 시 중복 검증
  - 동일한 username 또는 email 존재 시 BusinessException 발생
- 회원 정보 조회
  - 로그인된 사용자 정보(UserDetails 기반)로 DB 조회
- 회원 탈퇴
  - 본인 탈퇴 요청 시 softDeleteByUserId() 호출
    - 이미 탈퇴했거나 존재하지 않는 경우 예외 발생
- 회원 상태 변경
  - PENDING 상태일 때만 APPROVE 또는 REJECTED로 변경 가능
  - 이미 승인된 회원은 상태 변경 불가
- 예외 메시지 명시화
  - 예: "중복된 사용자 ID가 존재합니다.", "이미 탈퇴했거나 존재하지 않는 회원입니다.", "대기중인 회원만 상태를 변경할 수 있습니다."


## 테스트 커버리지

```
User Entity 단위 테스트의 jacoco 예시입니다.
라인 커버리지: 78%
브랜치 커버리지: 70%
```
---
아래처럼 User 모듈에서 원하는 테스트만 돌렸다.
```
./gradlew :module-user:test --tests "org.sparta.user.domain.entity.UserTest" :module-user:jacocoTestReport

start module-user/build/reports/jacoco/test/html/index.html
```
## 어려웠던 점 / 개선하고 싶은 부분(회고)


---

어떠한 방식으로 테스트 코드와 TDD 방식이 진행되는지는 알겠으나 이를 Jacoco 기준 100 퍼센트 적용하기에는 어렵다고 느껴졌다.

특히 금융이나 결제 쪽 도메인은 테스트 코드가 매우 까다로울텐데 모든 경우의 수를 다 고려해야만겠다는 생각이 들었던 것 같다.

아쉬운 점은 테스트 코드를 적는 시간보다 환경 설정 특히, JWT 쪽 문제로 인하여 온전히 User 테스트 코드 작성에 많은 시간을 사용하지 못하였다.

Bean, env, Config, Jwt 인증/인가 부분이 테스트 코드에서 어떻게 얽혀져 있는지에 대한 개념이 부족함을 느꼈다.

더욱 이론적인 공부가 필요하다고 스스로 느낀다.


## 체크리스트

## Step 2: 서비스 레이어 테스트와 Mock 활용

### **구현 체크리스트**

- [x]  서비스 레이어에 대한 테스트 코드 작성 (5개 이상 테스트 케이스)
- [x]  Mock 객체(@Mock, @InjectMocks)로 외부 의존성 격리
- [x]  정상 케이스 + 예외 케이스 각각 1개 이상 포함

</details>


</details>

<details>
  <summary>Order</summary>


</details>

<details>
  <summary>Hub</summary>

## Service 계층 테스트

### 구현 내용

- HubService의 `createHub()`, `getHub()`, `deleteHub()` 기능을 단위 테스트로 검증.
- 실제 DB 접근은 Mock Repository로 대체.

### 테스트 예시

```java
@ExtendWith(MockitoExtension.class)
class HubServiceTest {

    @Mock
    private HubRepository hubRepository;

    @InjectMocks
    private HubService hubService;

    @Test
    @DisplayName("허브 생성 시 ACTIVE 상태로 저장된다")
    void createHub_success() {
        Hub hub = Hub.builder().hubName("서울 허브").city("서울").build();
        given(hubRepository.save(any(Hub.class))).willReturn(hub);

        HubResponse result = hubService.createHub(new HubRequest("서울 허브", "서울"));

        assertThat(result.getHubName()).isEqualTo("서울 허브");
        assertThat(result.getStatus()).isEqualTo(HubStatus.ACTIVE);
    }

    @Test
    @DisplayName("존재하지 않는 허브 삭제 시 예외 발생")
    void deleteHub_notFound_throwsException() {
        given(hubRepository.findById(any())).willReturn(Optional.empty());

        assertThatThrownBy(() -> hubService.deleteHub(UUID.randomUUID()))
            .isInstanceOf(HubNotFoundException.class);
    }
}

```

### **체크리스트**

- [x]  Mock Repository 기반 단위 테스트 작성
- [x]  정상 + 예외 케이스 각각 포함
- [x]  서비스 로직 책임 명확화

### 회고

Mock Repository를 주입받아 외부 의존성을 제거하고 순수 비즈니스 로직만 검증했다.
예외 상황(존재하지 않는 허브 삭제 시 예외)까지 테스트하면서,
단위 테스트가 도메인 신뢰성을 보장하는 핵심임을 느꼈다.

</details>

<details>
  <summary>Product</summary>

## STEP

Step 2 - 서비스 레이어 테스트와 Mock 활용

---

## 각 Step별 학습 내용과 느낀 점
학습 내용

1. Mockito를 활용한 의존성 격리
@Mock, @InjectMocks 어노테이션을 사용하여 테스트 대상을 명확히 구분
특히 테스트 대상 클래스는 @InjectMocks로 주입한다는 점을 명확히 이해함

2. 메서드 호출 검증 (verify())의 활용
상태 검증뿐 아니라, 필요한 경우 행동 검증을 통해 의존성 간의 상호작용을 확인할 수 있음
다만 모든 테스트에서 행동 검증이 필요한지는 테스트의 목적에 따라 판단해야 함

3. 서비스 레이어의 책임 정립

 도메인 객체 생성 및 조율
 트랜잭션 관리
 비즈니스 규칙 검증 수행

느낀 점

Mock의 개념이 테스트 목적에 따라 달라진다는 점을 처음으로 명확히 이해함
단순히 “의존성을 가짜로 만든다”가 아니라, 테스트의 초점을 어디에 두느냐(행동 검증 vs 상태 검증)에 따라 Mock 사용 방식이 달라진다는 것을 체감함
이를 인식하며 테스트를 설계하니, 테스트 코드의 방향성과 의도가 더 분명해지는 경험을 할 수 있었음


---

## TDD 학습 과정 정리
Mock 설정 패턴 학습


```
// 1. 반환값 설정
given(categoryRepository.existsById(categoryId)).willReturn(true);

// 2. 동적 응답 설정 (저장된 객체 그대로 반환)
given(productRepository.save(any(Product.class)))
        .willAnswer(invocation -> invocation.getArgument(0));

// 3. 예외 발생 설정
given(productRepository.findById(invalidId))
        .willThrow(new BusinessException(PRODUCT_NOT_FOUND));
```

---

## 테스트 전략 설명
### 단위 테스트 

목표: ProductService의 비즈니스 로직만 격리하여 테스트

###  테스트 구성
- 테스트 대상: ProductService (실제 객체)
- Mock 객체: ProductRepository, CategoryRepository
- 테스트 도구: JUnit 5, Mockito, AssertJ

---

## 도메인 요구사항
### 상품 생성 (Product Creation)

요구사항
- 카테고리 검증
   - 존재하는 카테고리만 상품에 연결 가능
   - 카테고리가 없으면 CATEGORY_NOT_FOUND 예외 발생
   - 상품-재고 생성

- 상품 생성 시 Stock도 함께 생성 (Cascade.ALL)
   - Stock의 초기 수량은 요청의 quantity 값
   - Product와 Stock은 1:1 관계
- 가격 검증
   - 가격은 0 이상의 정수
   - Money VO로 금액 표현

### 상품 조회 (Product Retrieval)

정상 조회
- 유효한 ID로 조회 시 상품 정보 반환
- Stock 정보도 함께 반환

예외 처리
- 존재하지 않는 ID로 조회 시 PRODUCT_NOT_FOUND 예외 발생
- 논리적으로 삭제된 상품도 조회 가능 (deletedAt 확인)

---

## 테스트 커버리지

<img width="1125" height="354" alt="Image" src="https://github.com/user-attachments/assets/4665c383-96ed-4105-8c88-19974206d039" />

```
라인 커버리지: 63%
브랜치 커버리지: 48%
```
---

## 어려웠던 점 / 개선하고 싶은 부분
1. ArgumentCaptor 활용
어려웠던 점: "실제로 Stock이 함께 생성되었는가?"를 검증하고 싶었지만, Mock을 사용하면 실제 저장이 일어나지 않아서 어떻게 검증해야 할지 막막했습니다.
해결 방법: ArgumentCaptor를 사용해 save() 메서드에 전달된 실제 Product 객체를 캡처하여 검증했습니다.

2. 테스트 데이터 중복
어려웠던 점: 각 테스트마다 동일한 Product 객체를 반복적으로 생성하는 코드가 중복되었습니다.
해결 방법: ProductFixture 패턴을 도입하여 테스트 데이터 생성을 간소화했습니다.

개선하고 싶은 부분
JaCoCo 결과 브랜치 커버리지 48%로 분기에 대한 검증이 미흡했다는 것을 깨달았습니다. 
프로젝트를 더 진행해가면서 
예외 케이스와 엣지  케이스(null 체크, 경계값 등)에 대한 테스트를 추가하며 더 개선하고 싶습니다. 

---

## 체크리스트

## Step 2: 서비스 레이어 테스트와 Mock 활용

### **구현 체크리스트**

- [x]  서비스 레이어에 대한 테스트 코드 작성 (5개 이상 테스트 케이스)
- [x]  Mock 객체(@Mock, @InjectMocks)로 외부 의존성 격리
- [x]  정상 케이스 + 예외 케이스 각각 1개 이상 포함


</details>

----

### Step3

<details>
  <summary>User</summary>

## STEP

---

Step 3

## 각 Step별 학습 내용과 느낀 점

---

- 학습 내용:
  - @DataJpaTest를 활용해 Repository 단위 테스트 환경을 구성했다. 
  - H2 인메모리 데이터베이스를 통해 실제 쿼리 실행을 검증하고, CRUD 및 Soft Delete 기능의 동작을 테스트했다. 
  - 주요 테스트 시나리오:
    - 유저 저장 및 조회 (save, findById)
    - username, email 기반 조회 (findByUserName, findByEmail)
    - Soft Delete 기능 (softDeleteByUserId)
    - 삭제되지 않은 유저만 반환되는지 검증 (findAll)
  - 테스트 간 데이터 격리를 위해 각 테스트가 독립적으로 실행되도록 보장되었다. 
- 느낀 점:
  - 단순한 CRUD 테스트라고 생각했지만, 실제로는 엔티티의 삭제 정책(Soft Delete)을 테스트하면서 DB 레벨에서의 데이터 무결성을 검증할 필요성을 깨달았다. 
  - @DataJpaTest가 Repository 계층만 로드하기 때문에 테스트 속도가 매우 빠르고, 단위 테스트와 통합 테스트의 경계를 명확히 이해할 수 있었다. 
  - 엔티티의 변경 이력을 남기거나 삭제 정책을 분리하는 것이 도메인 설계에서 얼마나 중요한지 실감했다.

## TDD 학습 과정 정리

---

- Red 단계:
  - Soft Delete 동작이 Repository 계층에서 반영되지 않아 deletedAt이 null로 남는 문제 발생. 
- Green 단계:
  - @Query 기반의 softDeleteByUserId() 메서드를 구현하고, 테스트 통과 확인. 
- Refactor 단계:
  - 중복되는 유저 생성 코드를 createUser() 헬퍼 메서드로 추출하여 테스트 가독성을 높였다.


## 테스트 전략 설명

---

- Step3: Repository 단위 테스트
  - @DataJpaTest를 통해 Repository 계층만 로드하여 DB 쿼리 로직을 검증.
  - H2 기반으로 실제 DB I/O를 수행해 쿼리의 정확성 확인.
  - 각 테스트는 독립적이며 트랜잭션 롤백을 통해 데이터 격리를 유지.


## 도메인 요구사항

---

- 유저는 저장 후 findById, findByUserName, findByEmail로 조회 가능해야 한다.
- Soft Delete 수행 시 deletedAt이 설정되어야 한다.
- 삭제된 유저는 findAll() 결과에 포함되지 않아야 한다.
## 테스트 커버리지

```
User Entity 단위 테스트의 jacoco 예시입니다.
라인 커버리지: 78%
브랜치 커버리지: 70%
```
---
아래처럼 User 모듈에서 원하는 테스트만 돌렸다.
```
./gradlew :module-user:test --tests "org.sparta.user.domain.entity.UserTest" :module-user:jacocoTestReport

start module-user/build/reports/jacoco/test/html/index.html
```
## 어려웠던 점 / 개선하고 싶은 부분(회고)

---

어떠한 방식으로 테스트 코드와 TDD 방식이 진행되는지는 알겠으나 이를 Jacoco 기준 100 퍼센트 적용하기에는 어렵다고 느껴졌다.

특히 금융이나 결제 쪽 도메인은 테스트 코드가 매우 까다로울텐데 모든 경우의 수를 다 고려해야만겠다는 생각이 들었던 것 같다.

아쉬운 점은 테스트 코드를 적는 시간보다 환경 설정 특히, JWT 쪽 문제로 인하여 온전히 User 테스트 코드 작성에 많은 시간을 사용하지 못하였다.

Bean, env, Config, Jwt 인증/인가 부분이 테스트 코드에서 어떻게 얽혀져 있는지에 대한 개념이 부족함을 느꼈다.

더욱 이론적인 공부가 필요하다고 스스로 느낀다.


## 체크리스트
## Step 3: Repository 테스트

### **구현 체크리스트:**

- [x]  @DataJpaTest 또는 H2 DB 기반 Repository 테스트 (3개 이상)
- [x]  단순 조회, 저장, 삭제 등 기본 동작 검증
- [x]  테스트 간 데이터 격리 확인

</details>


</details>

<details>
  <summary>Order</summary>


</details>

<details>
  <summary>Hub</summary>

## Repository 테스트

### 구현 내용

- H2 인메모리 DB 기반의 JPA 테스트를 통해 저장, 조회, 상태 변경 검증.
- 실제 데이터베이스 환경과 동일하게 동작하는지 확인.

### 테스트 예시

```java
@DataJpaTest
class HubRepositoryTest {

    @Autowired
    private HubRepository hubRepository;

    @Test
    @DisplayName("허브 저장 및 조회 성공")
    void saveAndFind_success() {
        Hub hub = Hub.builder().hubName("서울 허브").city("서울").build();

        Hub saved = hubRepository.save(hub);
        Optional<Hub> found = hubRepository.findById(saved.getHubId());

        assertThat(found).isPresent();
        assertThat(found.get().getHubName()).isEqualTo("서울 허브");
    }

    @Test
    @DisplayName("삭제된 허브는 조회되지 않는다")
    void findActiveHub_excludesInactive() {
        Hub hub = Hub.builder().hubName("서울 허브").city("서울").build();
        hub.markAsDeleted();
        hubRepository.save(hub);

        List<Hub> result = hubRepository.findAllActive();

        assertThat(result).isEmpty();
    }
}

```

### **체크리스트**

- [x]  @DataJpaTest 적용
- [x]  저장/조회/삭제 로직 검증
- [x]  Soft Delete 반영 테스트

### 회고

@DataJpaTest를 활용하여 실제 DB 환경에서 저장, 조회, 삭제가 올바르게 작동하는지 확인했다.
Soft Delete가 반영된 조회 쿼리 테스트를 통해 데이터 정합성의 중요성을 체감했다.

</details>

<details>
  <summary>Product</summary>

## STEP

Step 3 - Repository 테스트

---

## 각 Step별 학습 내용과 느낀 점

@DataJpaTest의 특징

- Repository 관련 빈만 로드 
 - 각 테스트마다 트랜잭션 자동 롤백
 - H2 In-Memory DB 사용 (@ActiveProfiles("test"))
 - @AutoConfigureTestDatabase(replace = NONE) 설정으로 H2 DB 사용

TestEntityManager 활용
  - flush(): 쓰기 지연 SQL 즉시 실행
 - clear(): 1차 캐시 초기화
 - 영속성 컨텍스트 제어로 정확한 DB 동작 검증
 - 실제 DB INSERT/SELECT 쿼리 확인 가능

JPA Cascade와 연관관계
   - Product ↔ Stock의 1:1 관계 (@MapsId 사용)
   - Cascade.ALL로 자동 저장/삭제
   - Product 저장 시 Stock도 자동 저장 검증
   - Product ID와 Stock ID가 동일함을 검증

Aggregate Root의 책임
  - Product는 자신과 Stock의 일관성을 보장해야 함
- 도메인 불변식: "삭제된 상품은 판매할 수 없다"는 규칙을 코드로 강제해야 함
  
느낀점
Soft Delete 테스트를 통해 Aggregate Root의 책임과 연관 엔티티의 일관성 보장의 중요성을 배웠습니다.
Product의 삭제는 단순히 상태 변경이 아니라, 연관된 Stock의 도메인 일관성까지 함께 관리해야 함을 깨달았습니다.
이를 통해 Cascade와 영속성 컨텍스트의 동작 원리를 더 깊이 이해할 수 있었습니다.

---

## TDD 학습 과정 정리
1. RED → GREEN → REFACTOR 사이클 적용

단계 | 설명 | 코드/행동
-- | -- | --
 RED | 실패하는 테스트 작성 | save() 호출 후 findById() 시 연관 객체 미저장 오류 확인
GREEN | 최소한의 코드로 테스트 통과 | CascadeType.ALL, @MapsId 설정 추가 후 테스트 성공
REFACTOR | 중복 제거 및 명확성 향상 | flush() / clear() 패턴 정리 및 Fixture 도입


3. 영속성 컨텍스트 제어 패턴
```
// 1. 저장
Product savedProduct = productJpaRepository.save(product);

// 2. flush: 쓰기 지연 SQL 즉시 실행 (INSERT 쿼리 발생)
entityManager.flush();

// 3. clear: 1차 캐시 초기화 (메모리 캐시 비우기)
entityManager.clear();

// 4. 조회: 실제 DB에서 SELECT 쿼리 발생
Optional<Product> foundProduct = productJpaRepository.findById(savedProduct.getId());
```

---

## 테스트 전략 설명

### @DataJpaTest 통합 테스트 전략
목표: Repository의 CRUD 동작과 JPA 기능(Cascade, 영속성 등) 검증

### 테스트 구성
- 테스트 환경: @DataJpaTest
- DB: H2 In-Memory Database
- 도구: TestEntityManager, JUnit 5, AssertJ
- 격리: 각 테스트마다 트랜잭션 자동 롤백

---

## 도메인 요구사항

### Product-Stock 연관관계
요구사항
- 1:1 관계
  - Product와 Stock은 1:1 양방향 관계
  - Product 생성 시 Stock도 함께 생성
 
- Cascade 설정
  - Product 저장 시 Stock도 자동 저장 (Cascade.ALL)
  - Product 삭제 시 Stock도 함께 삭제
  - orphanRemoval = true로 고아 객체 제거/
  
- 논리적 삭제
  - 실제 DB에서 삭제하지 않음
  - deletedAt 필드에 삭제 시각 기록
  - 삭제된 상품도 조회 가능 (추후 복원 또는 이력 관리)
  - 상품이 삭제 되면 재고 정보도 비활성화 되어야한다.

---

## 테스트 커버리지

<img width="1104" height="339" alt="Image" src="https://github.com/user-attachments/assets/b3efbd15-d8a0-451f-86f6-d14623d8a448" />

```
라인 커버리지: 61 %
브랜치 커버리지: 34 %
```
---

## 어려웠던 점 / 개선하고 싶은 부분
어려웠던 점 
다른 테스트에서 저장한 데이터가 남아있어서 테스트가 실패하는 경우가 있었습니다. 또한 H2 DB 설정이 제대로 되지 않는 어려움이 있었습니다.

해결 방법:
@DataJpaTest가 자동으로 각 테스트마다 트랜잭션을 롤백하여 격리 보장
@ActiveProfiles("test")로 테스트 전용 H2 DB 설정 활성화
@AutoConfigureTestDatabase(replace = NONE)으로 H2 사용 명시
@Import(CategoryRepositoryImpl.class)로 필요한 빈 명시적 로드



---

## 체크리스트

### Step 3: Repository 테스트

### **구현 체크리스트:**

- [x]  @DataJpaTest 또는 H2 DB 기반 Repository 테스트 (3개 이상)
- [x]  단순 조회, 저장, 삭제 등 기본 동작 검증
- [x]  테스트 간 데이터 격리 확인

</details>


----

### Step4

<details>
  <summary>User</summary>

## STEP


Step 4

## 각 Step별 학습 내용과 느낀 점

---

- 학습 내용:
  - @WebMvcTest를 활용해 Controller 단위 테스트 환경을 구성했다. 
  - 실제 HTTP 요청 흐름(MockMvc)을 통해 Request → Controller → Service → Response로 이어지는 플로우를 검증했다. 
  - 주요 테스트 시나리오:
    - 회원가입 API 성공 시 200 OK 및 응답 JSON 검증 
    - 잘못된 입력값(email 누락 등) 시 400 Bad Request 반환 
    - 회원 단건 조회 API의 응답 데이터 검증 
    - 회원가입 승인(approve), 거절(reject) API 호출 시 정상 메시지 반환 
  - 보안 설정이 포함된 환경에서 테스트를 독립적으로 수행하기 위해 SecurityDisabledConfig를 Import하여 Spring Security를 비활성화했다.
  - 마지막으로 E2E 테스트까지 진행하여 전체적인 User의 흐름이 잘 진행되는지 확인하였다.
- 느낀 점:
  - 실제 API 요청 흐름을 시뮬레이션하면 Controller 계층의 책임과 유효성 검증 로직의 중요성을 명확히 이해할 수 있었다. 
  - MockMvc를 활용한 테스트는 빠르고 반복 가능성이 높아 회귀 테스트에 적합하다는 점을 체감했다. 
  
## TDD 학습 과정 정리

---

- Red 단계:
  - Controller 요청 시 DTO의 필드 유효성 검증이 누락되어 200이 반환되는 문제 발생. 
- Green 단계:
  - @Valid 및 DTO 내 필드 제약(@NotBlank 등)을 추가하여 400 응답 확인. 
- Refactor 단계:
  - 테스트 코드 내 반복되는 MockMvc 요청 로직을 메서드화하고, 공통 요청 객체 생성을 UserRequestFixture로 분리할 계획을 세움.

## 테스트 전략 설명

---

- Step4: Controller 단위 테스트 
  - @WebMvcTest로 Controller와 관련된 Bean만 로드하여 테스트 속도 최적화. 
  - Service 계층은 @MockitoBean으로 Mock 처리하여 외부 의존성 제거. 
  - MockMvc를 통해 HTTP 요청/응답 상태 및 JSON 필드 검증. 
  - Spring Security를 비활성화한 상태에서 Controller 로직만 순수하게 검증. 
  - E2E 테스트까지 확인 완료

## 도메인 요구사항

---

- 회원가입 시 유효한 입력이면 200 OK와 함께 회원명 반환 
- 잘못된 입력값 시 400 Bad Request 반환 
- 회원 단건 조회 시 정확한 정보(userName, email)를 포함해 반환 
- 회원가입 승인/거절 시 각각 "회원가입이 승인되었습니다.", "회원가입이 거절되었습니다." 메시지 반환

## 테스트 커버리지

```
User Entity 단위 테스트의 jacoco 예시입니다.
라인 커버리지: 78%
브랜치 커버리지: 70%
```
---
아래처럼 User 모듈에서 원하는 테스트만 돌렸다.
```
./gradlew :module-user:test --tests "org.sparta.user.domain.entity.UserTest" :module-user:jacocoTestReport

start module-user/build/reports/jacoco/test/html/index.html
```
## 어려웠던 점 / 개선하고 싶은 부분(회고)

---

어떠한 방식으로 테스트 코드와 TDD 방식이 진행되는지는 알겠으나 이를 Jacoco 기준 100 퍼센트 적용하기에는 어렵다고 느껴졌다.

특히 금융이나 결제 쪽 도메인은 테스트 코드가 매우 까다로울텐데 모든 경우의 수를 다 고려해야만겠다는 생각이 들었던 것 같다.

아쉬운 점은 테스트 코드를 적는 시간보다 환경 설정 특히, JWT 쪽 문제로 인하여 온전히 User 테스트 코드 작성에 많은 시간을 사용하지 못하였다.

Bean, env, Config, Jwt 인증/인가 부분이 테스트 코드에서 어떻게 얽혀져 있는지에 대한 개념이 부족함을 느꼈다.

더욱 이론적인 공부가 필요하다고 스스로 느낀다.


## 체크리스트
- 각 스템별 해당되는 체크리스트를 복사해주세요

## Step 4: API 테스트와 통합 테스트

- [x]  Controller 단위 테스트 작성 (3개 이상 엔드포인트 테스트)
- [x]  MockMvc로 HTTP 요청/응답 상태 및 JSON 검증
- [x]  간단한 통합 테스트(E2E) 1개 작성 (ex: 주문 생성 → 조회 플로우)

</details>

</details>

<details>
  <summary>Order</summary>


</details>

<details>
  <summary>Hub</summary>

## API 통합 테스트(Controller 계층)

### 구현 내용

- `MockMvc` 기반의 Controller 통합 테스트 작성.
- 실제 요청-응답 구조와 예외 처리를 검증.
- Hub 등록, 조회, 삭제(INACTIVE 변경) 플로우 검증.

### 테스트 예시

```java
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class HubControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private HubRepository hubRepository;

    @Test
    @DisplayName("허브 생성 성공 - ACTIVE 상태로 생성")
    void createHub_success() throws Exception {
        String request = """
            { "hubName": "서울 허브", "city": "서울특별시" }
        """;

        mockMvc.perform(post("/api/hubs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.meta.result").value("SUCCESS"))
            .andExpect(jsonPath("$.data.hubName").value("서울 허브"))
            .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("허브 삭제 성공 - INACTIVE로 변경")
    void deleteHub_success() throws Exception {
        Hub hub = hubRepository.saveAndFlush(
            Hub.builder().hubName("테스트 허브").city("서울").build()
        );

        mockMvc.perform(delete("/api/hubs/{hubId}", hub.getHubId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.meta.result").value("SUCCESS"))
            .andExpect(jsonPath("$.data.status").value("INACTIVE"));
    }

    @Test
    @DisplayName("존재하지 않는 허브 삭제 시 404 반환")
    void deleteHub_notFound() throws Exception {
        UUID fakeId = UUID.randomUUID();

        mockMvc.perform(delete("/api/hubs/{hubId}", fakeId))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.meta.message").value("Hub not found"));
    }
}

```

### **체크리스트**

- [x]  Controller 단위/통합 테스트 작성
- [x]  정상 + 예외 케이스 모두 검증
- [x]  JSON 응답 구조 및 상태 코드 확인

### 회고

MockMvc를 사용해 Controller 계층의 요청/응답을 엔드 투 엔드로 검증했다.
성공/실패 케이스를 모두 포함하면서, 실제 사용자가 API를 사용하는 플로우를 시뮬레이션할 수 있었다.

</details>

<details>
  <summary>Product</summary>

## STEP

Step 4 - API 테스트와 e2e 통합 테스트 진행 

---

## 각 Step별 학습 내용과 느낀 점

이번 단계에서는 Controller 단위 테스트와 E2E 통합 테스트를 모두 진행하며,
테스트의 목적과 범위에 따라 접근 방식이 달라야 함을 경험을 통해 이해하게 되었다.

단위 테스트(@WebMvcTest)는 빠른 피드백과 예외 처리 검증에 매우 효율적이었고,
실제 DB를 사용하지 않아 테스트 실행 속도가 비교적 빠른 것 같은 느낌이 들었다.

E2E 통합 테스트(@SpringBootTest + RestAssured)는 실제 HTTP 요청을 통해
컨트롤러 → 서비스 → 레포지토리 → DB 흐름을 완전하게 검증할 수 있었으며,
서비스 로직이 서로 연결된 상태에서 발생할 수 있는 버그를 조기에 확인할 수 있다는 이점이 있었다.

항상 단위 테스트만 작성해왔는데, 이번에 처음으로 Controller 단위 테스트와 E2E 통합 테스트를 경험했다.
전체 요청 → 서비스 → DB까지 이어지는 플로우를 테스트해보면서,
단위 테스트만으로는 놓칠 수 있는 실제 동작 검증의 중요성을 실감할 수 있었다.
앞으로는 단위 테스트와 통합 테스트를 적절히 활용하여, 더 신뢰성 있는 코드를 작성하고 싶다.

---

## TDD 학습 과정 정리

1.테스트 시나리오 작성 → 실패(Fail) 확인
2.Controller/Service/Repository 구현 → 테스트 통과(Pass)
3.예외 케이스 추가 및 검증 (404, 400 등)
4.전체 플로우 E2E 테스트로 통합 검증
5.테스트 통과 후 리팩토링 및 커버리지 확인

---

## 테스트 전략 설명

### Controller 단위 테스트 (@WebMvcTest)
목표: Controller의 HTTP 요청/응답 처리만 격리하여 테스트
 테스트 구성
- 테스트 환경: @WebMvcTest(ProductController.class)
- 도구: MockMvc, @MockBean
- Mock 객체: ProductService
- 특징: MVC 관련 빈만 로드, 빠른 실행
- 예외 처리 검증: @Import(ApiControllerAdvice.class)로 전역 예외 핸들러 포함

 주요 검증 시나리오
  - 상품 생성 성공
  - 존재하지 않는 카테고리로 생성 시 404 반환
  - 상품 단건 조회 성공
  - 존재하지 않는 상품 조회 시 404 반환
  - 상품 수정 성공
  - 상품 삭제 성공 / 존재하지 않는 상품 삭제 시 404


E2E 통합 테스트 
목표: Controller → Service → Repository → DB 전체 레이어 통합 검증
테스트 구성
- 환경: @SpringBootTest(webEnvironment = RANDOM_PORT)
- 도구: RestAssured
- DB: H2 In-Memory Database
- 트랜잭션 관리: TransactionTemplate으로 데이터 초기화

주요 검증 시나리오
- 상품 생성 → 조회 → 수정 → 삭제 전체 플로우 검증
- 존재하지 않는 카테고리로 생성 시 404
- 존재하지 않는 상품 조회 시 404
- 유효하지 않은 요청(price < 0) 시 400
- 상품 삭제 시 연관된 Stock 상태가 UNAVAILABLE로 변경


---

## 도메인 요구사항


기능 | API | 요청 DTO | 응답 DTO | 검증 및 예외 처리
-- | -- | -- | -- | --
상품 생성 | POST /api/products | ProductRequest.Create | ProductResponse.Create | 카테고리 존재 확인, 가격 ≥ 0, 수량 ≥ 0
상품 조회 | GET /api/products/{id} | UUID | ProductResponse.Detail | 존재하지 않으면 404 PRODUCT_NOT_FOUND
상품 수정 | PATCH /api/products/{id} | ProductRequest.Update | ProductResponse.Update | 존재 확인 및 수정 가능한 필드만 변경
상품 삭제 | DELETE /api/products/{id} | UUID | 성공 메시지 | 논리적 삭제

---

## 어려웠던 점 / 개선하고 싶은 부분

이번 단계에서 처음으로 Controller 단위 테스트와 E2E 통합 테스트를 작성하면서,
단순히 테스트 로직만 작성하는 것이 아니라 RestAssured, MockMvc, TransactionTemplate, @WebMvcTest, @SpringBootTest 등
여러 기능과 어노테이션을 학습하고 적용하는 과정이 쉽지 않았다.
특히 각 기능의 역할과 테스트 범위를 이해하고, 실제 HTTP 요청과 DB 트랜잭션을 연동해 검증하는 과정에서 어려움을 느꼈다.


---

## 체크리스트
- 각 스템별 해당되는 체크리스트를 복사해주세요

## Step 4: API 테스트와 통합 테스트

- [x]  Controller 단위 테스트 작성 (3개 이상 엔드포인트 테스트)
- [x]  MockMvc로 HTTP 요청/응답 상태 및 JSON 검증
- [x]  간단한 통합 테스트(E2E) 1개 작성 (ex: 주문 생성 → 조회 플로우)




</details>


---

### Step5

<details>
  <summary>User</summary>

## STEP

---

Step5

## 각 Step별 학습 내용과 느낀 점

---

- 학습 내용:
  - 기존 Step 1~4에서 작성한 User 도메인, Repository, Controller 테스트 코드를 전반적으로 리팩토링했다. 
  - Fixture 패턴(UserFixture) 을 도입하여 테스트 데이터 생성을 공통화하고, 테스트 간 일관성을 유지하며 중복 코드를 제거했다. 
  - @ParameterizedTest와 @CsvSource를 활용해 회원 상태 전이(updateStatus) 로직의 경계 테스트를 강화했다. 
  - jacocoTestReport를 통해 전체 모듈의 커버리지를 확인했다. 
- 느낀 점:
  - 처음에는 단순한 리팩토링처럼 보였지만, Fixture를 도입하면서 테스트 코드 구조가 명확해지고 유지보수성이 대폭 개선되었다. 
  - 경계 테스트를 추가하며 도메인 제약조건을 테스트가 보장하는 구조의 중요성을 실감했다. 
  - 테스트 코드는 단순히 기능 확인용이 아니라, 요구사항 명세의 일부로 작동해야 한다는 인식을 갖게 되었다.

## TDD 학습 과정 정리

---

- Red 단계:
  - User 상태 전이 테스트에서 REJECTED 상태를 다시 변경할 수 있는 문제 발견. 
- Green 단계:
  - updateStatus() 내 상태 제약 조건 추가 (PENDING 상태에서만 전이 가능). 
- Refactor 단계:
  - 테스트 중복 제거 및 Fixture 도입 
  - 경계 테스트(approve, reject, invalid transition) 케이스 추가 
  - 전체 테스트를 실행 후 커버리지 보고서를 통해 누락 구간 보완


## 테스트 전략 설명

---

- Step5에선 리팩토링 및 Fixture, Jacoco 이용하여 범위 점검 및 보완을 수행하였습니다.


## 도메인 요구사항

---

- 회원 상태 전이 제약 강화 
  - PENDING 상태에서만 APPROVE 또는 REJECTED로 변경 가능 
  - 이미 승인/거절된 회원은 상태 변경 불가 (BusinessException 발생)
- 테스트 공통화 및 중복 제거 
  - UserFixture로 공통 테스트 데이터 생성 
  - ParameterizedTest를 활용한 상태 전이 경계 검증


## 테스트 커버리지
```
User Entity 단위 테스트의 jacoco 예시입니다.
라인 커버리지: 78%
브랜치 커버리지: 70%
```
---
아래처럼 User 모듈에서 원하는 테스트만 돌렸다.
```
./gradlew :module-user:test --tests "org.sparta.user.domain.entity.UserTest" :module-user:jacocoTestReport

start module-user/build/reports/jacoco/test/html/index.html
```
## 어려웠던 점 / 개선하고 싶은 부분(회고)

---

어떠한 방식으로 테스트 코드와 TDD 방식이 진행되는지는 알겠으나 이를 Jacoco 기준 100 퍼센트 적용하기에는 어렵다고 느껴졌다.

특히 금융이나 결제 쪽 도메인은 테스트 코드가 매우 까다로울텐데 모든 경우의 수를 다 고려해야만겠다는 생각이 들었던 것 같다.

아쉬운 점은 테스트 코드를 적는 시간보다 환경 설정 특히, JWT 쪽 문제로 인하여 온전히 User 테스트 코드 작성에 많은 시간을 사용하지 못하였다.

Bean, env, Config, Jwt 인증/인가 부분이 테스트 코드에서 어떻게 얽혀져 있는지에 대한 개념이 부족함을 느꼈다.

더욱 이론적인 공부가 필요하다고 스스로 느낀다.


## 체크리스트
- 각 스템별 해당되는 체크리스트를 복사해주세요

## Step 5: 복잡한 비즈니스 로직과 리팩토링

- [x]  중복 테스트 코드 Fixture로 공통화
- [x]  @ParameterizedTest 활용한 경계 테스트 1개 이상
- [x]  리팩토링 후 테스트 전부 통과 확인
- [x]  README에 TDD 적용 회고 작성 (3~5줄 이상)

## 선택 과제 (보너스)

- [x]  Jacoco로 테스트 커버리지 리포트 확인
- [ ]  Spring REST Docs나 Mutation Testing(PIT) 간단히 시도

</details>


</details>

<details>
  <summary>Order</summary>


</details>

<details>
  <summary>Hub</summary>

## 리팩토링 및 테스트 고도화

### 구현 내용

- 테스트 공통 로직 Fixture로 분리
- 반복되는 MockMvc 요청/응답 패턴을 메서드화
- 모든 계층 테스트가 통과한 상태에서 Service 리팩토링 진행

### 예시: 리팩토링 전/후

```java
// Before
@Transactional
public HubResponse deleteHub(UUID hubId) {
    Hub hub = hubRepository.findById(hubId)
        .orElseThrow(() -> new HubNotFoundException());
    hub.markAsDeleted();
    hubRepository.save(hub);
    return HubResponse.from(hub);
}

// After
@Transactional
public HubResponse deleteHub(UUID hubId) {
    Hub hub = hubRepository.findByIdOrThrow(hubId);
    hub.markAsDeleted();
    return HubResponse.from(hubRepository.saveAndFlush(hub));
}

```

> 현재는 테스트 코드 내에 인라인 방식으로 Fixture를 작성했지만,
추후 테스트 규모가 확장될 경우 중복 제거 및 유지보수성을 위해
아래 예시와 같은 별도 Fixture 클래스를 도입할 계획
> 

```java
public class HubFixture {
    public static Hub activeHub() { ... }
    public static Hub inactiveHub() { ... }
    public static HubRequest createRequest() { ... }
    public static Hub customHub(String name, String city) { ... }
}

```

```java
	/* Fixture 사용 시 테스트 코드 */
    @Test
    @DisplayName("허브 생성 성공 - ACTIVE 상태로 생성")
    void createHub_success() throws Exception {
        // given
        var request = HubFixture.createRequest();

        // when & then
        mockMvc.perform(post("/api/hubs")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "hubName": "%s", "city": "%s" }
                """.formatted(request.hubName(), request.city())))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.meta.result").value("SUCCESS"))
            .andExpect(jsonPath("$.data.hubName").value(request.hubName()))
            .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

```

> 불필요한 중복 제거, 명시적 flush로 트랜잭션 안정성 확보.
> 
> 
> 모든 테스트 통과 후 리팩토링 완료.
> 

### **체크리스트**

- [x]  Fixture 분리 및 재사용
- [x]  서비스 리팩토링 후 테스트 전부 통과
- [x]  README에 회고 추가

### 회고

중복되는 데이터 생성 코드를 정리하고, 명시적인 flush로 트랜잭션 안정성을 확보했다.
현재는 인라인 Fixture를 사용하지만, 규모 확장 시 HubFixture 클래스로 분리하여
유지보수성을 높일 계획이다.

</details>

<details>
  <summary>Product</summary>

## STEP

Step 5 - 복잡한 비즈니스 로직과 리팩토링

---

## 각 Step별 학습 내용과 느낀 점

Step 5에서는 **재고 예약 시스템**을 TDD 방식으로 구현하며, 복잡한 비즈니스 로직과 동시성 문제를 다뤘습니다.

* **학습 내용**

  * 주문 생성 시 재고 예약, 결제 완료 시 예약 확정, 주문 취소 시 예약 취소
  * 재고 상태 자동 전환(IN_STOCK, RESERVED_ONLY, OUT_OF_STOCK)
  * 낙관적 락(@Version)과 자동 재시도(@Retryable)로 동시성 문제 해결
  * Testcontainers를 활용한 실제 DB 환경에서 동시성 검증
  * Fixture 패턴(ProductFixture)으로 반복 테스트 코드 리팩토링 진행 
 
* **느낀 점**

실제 운영 환경과 유사한 PostgreSQL 컨테이너에서 동시성 문제를 검증해보면서, 단위 테스트만으로는 놓치기 쉬운 복잡한 상황까지 미리 확인할 수 있다는 것을 경험했습니다.
이전 프로젝트에서는 재고 동시성 테스트를 직접 JMeter로 수행했었는데, 이번에는 충분히 테스트 코드만으로도 미리 검증할 수 있다는 점을 새롭게 알게 되었습니다.

---

## TDD 학습 과정 정리

* **RED → GREEN → REFACTOR 사이클**

  1. RED: 실패하는 테스트 작성

     ```java
     @Test
     @DisplayName("가용 재고가 충분하면 예약에 성공한다")
     void reserve_WithSufficientStock_ShouldSucceed() {
         Product product = ProductFixture.withStock(100);
         Stock stock = product.getStock();
         stock.reserve(30); // 실패
         assertThat(stock.getReservedQuantity()).isEqualTo(30);
     }
     ```
  2. GREEN: 테스트를 통과하는 최소 코드 작성
  3. REFACTOR: 중복 검증 로직 추출, 상태 전환 로직 캡슐화, Fixture 패턴 적용

* **계층별 TDD 적용**

  * 도메인 테스트: Stock 도메인 객체 검증
  * 서비스 테스트: StockService 로직 검증, Repository Mock 사용
  * 통합 테스트: PostgreSQL 환경에서 동시성 검증, ExecutorService로 100개 스레드 시뮬레이션

---

## 테스트 전략 설명

* **단위 테스트 (StockTest.java)**

  * 순수 Java 객체 테스트, Mock 없음
  * 17개 테스트 케이스: 재고 예약, 확정, 취소, 상태 전환, 예외 처리
* **서비스 테스트 (StockServiceTest.java)**

  * Mock으로 Repository 격리, 9개 테스트 케이스
* **통합 테스트 (StockConcurrencyTest.java)**

  * Testcontainers PostgreSQL 환경, ExecutorService로 동시 요청 시뮬레이션
  * @Retryable + 낙관적 락 충돌 처리 검증

---

## 도메인 요구사항

* **재고 예약**

  * 가용 재고 확인 후 예약
  * 가용 재고 부족 시 `INSUFFICIENT_STOCK` 예외 발생
* **예약 확정**

  * 예약된 재고를 실제로 차감
  * 잘못된 확정 시 `INVALID_RESERVATION_CONFIRM` 예외
* **예약 취소**

  * 예약만 취소, 실제 재고 유지
  * 잘못된 취소 시 `INVALID_RESERVATION_CANCEL` 예외
* **재고 상태 자동 전환**

  * `OUT_OF_STOCK`: 실물 재고 0
  * `RESERVED_ONLY`: 실물은 있으나 가용 재고 0
  * `IN_STOCK`: 가용 재고 > 0

---

## 어려웠던 점 / 개선하고 싶은 부분

* **동시성 테스트 불안정**: H2 DB 환경에서 낙관적 락 동작 불안정 → PostgreSQL Testcontainers로 해결
* **복잡한 재고 예약 정책**: 예약 → 확정 2단계 패턴 필요성 체감
* **테스트 데이터 관리**: Fixture 패턴으로 반복 코드 제거

---

## 체크리스트

* [x] 중복 테스트 코드 Fixture로 공통화
* [x] @ParameterizedTest 활용한 경계 테스트 1개 이상
* [x] 리팩토링 후 테스트 전부 통과 확인
* [x] README에 TDD 적용 회고 작성 (3~5줄 이상)


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
라인 커버리지: 89%
브랜치 커버리지: 73%
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
