## STEP
<!-- 현재 진행 중인 스텝을 작성해주세요 (예: Step 1, Step 2) -->

---

Step 1~5

## 각 Step별 학습 내용과 느낀 점
<!-- 각 단계에서 학습한 내용과 느낀 점을 작성해주세요 -->

---

- Step1
  - 학습 내용:
    - User 도메인의 핵심 로직(생성, 상태 변경, 필드 수정)을 중심으로 도메인 단위 테스트를 작성했다. 
    - Given-When-Then 구조를 명확히 구분하여 테스트 의도를 표현하고, 예외 상황(BusinessException)을 검증했다. 
  - 느낀 점:
    - 처음에는 단순한 테스트처럼 보였지만, 생성자 검증과 상태 전이 로직을 명확히 정의하는 과정에서 도메인 요구사항이 더 구체화되었다. 
    - 유효성 검사를 코드로 명시함으로써 안전한 도메인 모델링의 중요성을 체감했다. 
- Step2
  - 학습 내용:
    - User 도메인 테스트(Step1)에서 확장하여 UserService의 비즈니스 로직을 단위 테스트했다. 
    - 외부 의존성을 가진 Repository, PasswordEncoder, SecurityService를 Mockito의 @Mock으로 대체하여 독립적인 테스트 환경을 구성했다. 
    - 회원가입, 조회, 탈퇴, 상태 변경 등의 핵심 기능을 시나리오 단위로 검증하고, 정상 케이스와 예외 케이스를 모두 포함했다. 
    - Mock 기반 테스트를 통해 실제 DB나 보안 로직에 의존하지 않고도 서비스 계층의 흐름을 명확히 검증할 수 있었다. 
  - 느낀 점:
    - 처음엔 Mock 객체 설정이 다소 복잡했지만, given-when-then 구조를 통해 테스트 의도를 분리하니 가독성이 높아졌다. 
    - 실제 서비스 코드에서 Repository나 Encoder가 호출되는 시점을 명확히 파악할 수 있어, 비즈니스 로직 설계 검증에 큰 도움이 되었다. 
    - 단순한 단위 테스트를 넘어 예외 처리와 흐름 제어까지 검증하는 중요성을 체감했다.
- Step3
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
- Step4
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
    - 실제 API 요청 흐름을 시뮬레이션하면서 Controller 계층의 책임과 유효성 검증 로직의 중요성을 명확히 이해할 수 있었다. 
    - MockMvc를 활용한 테스트는 빠르고 반복 가능성이 높아 회귀 테스트에 적합하다는 점을 체감했다. 
- Step5
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
<!--어떻게 적용했는지 정리해주세요 -->

---
- Step1
  - Red 단계:
    - User 생성 시 유효성 검사를 수행하지 않아 예외가 발생하지 않는 문제를 발견했다.
  - Green 단계:
    - User.create() 내부에 username, email, slackId 검증 로직을 추가하고, 테스트를 통과시켰다.
  - Refactor 단계:
    - 중복되는 생성 코드들을 UserFixture로 추출하여 재사용성을 높였다. 
    - 이후 updateStatus() 경계 테스트를 통해 로직 누락을 점검했다.
- Step2
  - Red 단계:
    - UserService.signup()에서 중복 사용자 검증 로직이 없어서 테스트 실패 
    - 탈퇴 요청 시 예외 처리가 누락되어 BusinessException 미발생 
  - Green 단계:
    - userRepository.findByUserName() 및 softDeleteByUserId()의 반환값에 따라 예외를 발생시키는 로직 추가 
    - 승인/거절 처리 시 UserStatusEnum.PENDING 상태만 변경 가능하도록 수정 
  - Refactor 단계:
    - 중복되는 Mock 설정을 정리하고 verify()로 호출 여부를 명시적으로 검증 
    - 테스트 명세서 역할을 하도록 각 테스트에 명확한 @DisplayName을 추가
- Step3
  - Red 단계:
    - Soft Delete 동작이 Repository 계층에서 반영되지 않아 deletedAt이 null로 남는 문제 발생. 
  - Green 단계:
    - @Query 기반의 softDeleteByUserId() 메서드를 구현하고, 테스트 통과 확인. 
  - Refactor 단계:
    - 중복되는 유저 생성 코드를 createUser() 헬퍼 메서드로 추출하여 테스트 가독성을 높였다.
- Step4
  - Red 단계:
    - Controller 요청 시 DTO의 필드 유효성 검증이 누락되어 200이 반환되는 문제 발생. 
  - Green 단계:
    - @Valid 및 DTO 내 필드 제약(@NotBlank 등)을 추가하여 400 응답 확인. 
  - Refactor 단계:
    - 테스트 코드 내 반복되는 MockMvc 요청 로직을 메서드화하고, 공통 요청 객체 생성을 UserRequestFixture로 분리할 계획을 세움.
- Step5
  - Red 단계:
    - User 상태 전이 테스트에서 REJECTED 상태를 다시 변경할 수 있는 문제 발견. 
  - Green 단계:
    - updateStatus() 내 상태 제약 조건 추가 (PENDING 상태에서만 전이 가능). 
  - Refactor 단계:
    - 테스트 중복 제거 및 Fixture 도입 
    - 경계 테스트(approve, reject, invalid transition) 케이스 추가 
    - 전체 테스트를 실행 후 커버리지 보고서를 통해 누락 구간 보완


## 테스트 전략 설명
<!-- 어떤 테스트 전략을 사용했는지 설명해주세요 (단위 테스트, 통합 테스트 등) -->

---

- Step1: 단위 테스트(Unit Test)
  - User 엔티티의 생성 및 상태 변경 로직이 명세대로 작동하는지 검증
  - 정상 입력과 예외 입력 케이스 분리 
  - 파라미터라이즈드 테스트로 상태 전이 경계값 검증 
  - Fixture를 활용한 중복 제거
- Step2: 단위 테스트 (Unit Test)
  - UserService의 메서드를 독립적으로 테스트하며 외부 의존성을 Mock 처리
  - 정상 케이스 + 예외 케이스 각각 검증
  - Mock을 통한 Repository, Encoder 호출 여부와 인자값 확인
  - 비즈니스 규칙(회원가입 중복, 탈퇴 예외, 상태 전이)을 명시적으로 검증
- Step3: Repository 단위 테스트
  - @DataJpaTest를 통해 Repository 계층만 로드하여 DB 쿼리 로직을 검증.
  - H2 기반으로 실제 DB I/O를 수행해 쿼리의 정확성 확인.
  - 각 테스트는 독립적이며 트랜잭션 롤백을 통해 데이터 격리를 유지.
- Step4: Controller 단위 테스트 
  - @WebMvcTest로 Controller와 관련된 Bean만 로드하여 테스트 속도 최적화. 
  - Service 계층은 @MockitoBean으로 Mock 처리하여 외부 의존성 제거. 
  - MockMvc를 통해 HTTP 요청/응답 상태 및 JSON 필드 검증. 
  - Spring Security를 비활성화한 상태에서 Controller 로직만 순수하게 검증. 
  - E2E 테스트까지 확인 완료
- Step5에선 리팩토링 및 Fixture, Jacoco 이용하여 범위 점검 및 보완을 수행하였습니다.


## 도메인 요구사항
<!-- 구현한 도메인 요구사항을 작성해주세요 -->

---

- Step1:
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
- Step2: 
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
- Step3:
  - 유저는 저장 후 findById, findByUserName, findByEmail로 조회 가능해야 한다.
  - Soft Delete 수행 시 deletedAt이 설정되어야 한다.
  - 삭제된 유저는 findAll() 결과에 포함되지 않아야 한다.
- Step4:
  - 회원가입 시 유효한 입력이면 200 OK와 함께 회원명 반환 
  - 잘못된 입력값 시 400 Bad Request 반환 
  - 회원 단건 조회 시 정확한 정보(userName, email)를 포함해 반환 
  - 회원가입 승인/거절 시 각각 "회원가입이 승인되었습니다.", "회원가입이 거절되었습니다." 메시지 반환
- Step5:
  - 회원 상태 전이 제약 강화 
    - PENDING 상태에서만 APPROVE 또는 REJECTED로 변경 가능 
    - 이미 승인/거절된 회원은 상태 변경 불가 (BusinessException 발생)
  - 테스트 공통화 및 중복 제거 
    - UserFixture로 공통 테스트 데이터 생성 
    - ParameterizedTest를 활용한 상태 전이 경계 검증


## 테스트 커버리지
<!-- 테스트 커버리지 결과를 첨부해주세요 -->
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
<!-- TDD를 진행하면서 어려웠던 점이나 개선하고 싶은 부분을 작성해주세요 -->

---

어떠한 방식으로 테스트 코드와 TDD 방식이 진행되는지는 알겠으나 이를 Jacoco 기준 100 퍼센트 적용하기에는 어렵다고 느껴졌다.

특히 금융이나 결제 쪽 도메인은 테스트 코드가 매우 까다로울텐데 모든 경우의 수를 다 고려해야만겠다는 생각이 들었던 것 같다.

아쉬운 점은 테스트 코드를 적는 시간보다 환경 설정 특히, JWT 쪽 문제로 인하여 온전히 User 테스트 코드 작성에 많은 시간을 사용하지 못하였다.

Bean, env, Config, Jwt 인증/인가 부분이 테스트 코드에서 어떻게 얽혀져 있는지에 대한 개념이 부족함을 느꼈다.

더욱 이론적인 공부가 필요하다고 스스로 느낀다.


## 체크리스트
- 각 스템별 해당되는 체크리스트를 복사해주세요

<details>
  <summary>체크리스트</summary>

## Step 1:도메인 모델 테스트

### **구현 체크리스트:**

- [ ]  도메인 모델 클래스와 테스트 코드 작성 (5개 이상 테스트 케이스)
- [ ]  각 테스트에 Given-When-Then 구조 명시
- [ ]  TDD 사이클을 경험한 느낀 점을 README에 간단히 기록

## Step 2: 서비스 레이어 테스트와 Mock 활용

### **구현 체크리스트**

- [ ]  서비스 레이어에 대한 테스트 코드 작성 (5개 이상 테스트 케이스)
- [ ]  Mock 객체(@Mock, @InjectMocks)로 외부 의존성 격리
- [ ]  정상 케이스 + 예외 케이스 각각 1개 이상 포함

## Step 3: Repository 테스트

### **구현 체크리스트:**

- [ ]  @DataJpaTest 또는 H2 DB 기반 Repository 테스트 (3개 이상)
- [ ]  단순 조회, 저장, 삭제 등 기본 동작 검증
- [ ]  테스트 간 데이터 격리 확인

## Step 4: API 테스트와 통합 테스트

- [ ]  Controller 단위 테스트 작성 (3개 이상 엔드포인트 테스트)
- [ ]  MockMvc로 HTTP 요청/응답 상태 및 JSON 검증
- [ ]  간단한 통합 테스트(E2E) 1개 작성 (ex: 주문 생성 → 조회 플로우)

## Step 5: 복잡한 비즈니스 로직과 리팩토링

- [ ]  중복 테스트 코드 Fixture로 공통화
- [ ]  @ParameterizedTest 활용한 경계 테스트 1개 이상
- [ ]  리팩토링 후 테스트 전부 통과 확인
- [ ]  README에 TDD 적용 회고 작성 (3~5줄 이상)

## 선택 과제 (보너스)

- [ ]  Jacoco로 테스트 커버리지 리포트 확인
- [ ]  Spring REST Docs나 Mutation Testing(PIT) 간단히 시도

</details>
