# 테스트 작성 가이드

> 이 문서는 프로젝트의 테스트 코드 작성 시 따라야 할 규칙과 패턴을 정의합니다.

---

## 📋 목차

1. [테스트 네이밍 규칙](#테스트-네이밍-규칙)
2. [Fixture 사용법](#fixture-사용법)
3. [Mock 사용 가이드](#mock-사용-가이드)

---

## 테스트 네이밍 규칙

### 기본 원칙

#### 1. 테스트 메서드 이름

이름만으로 **"무엇을", "어떤 상황에서", "어떤 결과를 기대하는지"** 알 수 있게 작성합니다.

**형식:**
```
[메서드명]_[상황]_[기대결과]
```

**예시:**
```java
createHub_WithValidInput_ShouldSucceed()
createHub_WithDuplicateName_ShouldThrowException()
deactivateHub_AlreadyInactive_ShouldThrowException()
getHub_WithValidId_ReturnsHubResponse()
```

#### 2. 테스트 코드 위치

**"미러 구조"** : 테스트 코드는 실제 소스 구조를 그대로 따라갑니다.

```
test/java/com/sparta/rocket_delivery/
 ┣ 에다가 메인 소스코드 패키지 구조 그대로 만들어서 테스트합니다.
```

> **장점:**
> - 어떤 테스트가 어떤 코드를 검증하는지 바로 이해할 수 있습니다.
> - 패키지별로 독립된 책임을 가진 테스트로 관리합니다.

<details>
<summary><b>패키지 네이밍 규칙 상세보기</b></summary>

| 유형 | 네이밍 규칙 | 예시 |
|------|------------|------|
| 도메인 단위 테스트 | `{EntityName}Test` | `HubTest`, `OrderTest` |
| 서비스 테스트 | `{EntityName}ServiceTest` | `HubServiceTest` |
| 레포지토리 테스트 | `{EntityName}RepositoryTest` | `HubRepositoryTest` |
| 컨트롤러 테스트 | `{EntityName}ControllerTest` | `HubControllerTest` |
| 통합 테스트 | `{EntityName}IntegrationTest` | `HubIntegrationTest` |
| Fixture 클래스 | `{EntityName}Fixture` | `HubFixture` |

</details>

#### 3. 단일 책임 원칙

테스트는 **하나의 시나리오만** 검증합니다.

#### 4. 중복 제거

중복된 데이터는 **Fixture**로 통일합니다.

---

### @DisplayName 규칙

- **문장형 한글 표현** 사용
- **"~하면 ~한다"** 구조로 요구사항 표현

```java
@DisplayName("허브를 정상적으로 생성하면 저장된다")
@DisplayName("중복된 이름으로 허브를 생성하면 예외가 발생한다")
```

---

### Given-When-Then 패턴

모든 테스트는 **상황(Given)**, **행동(When)**, **결과(Then)**를 시각적으로 분리해야 합니다.

```java
@Test
@DisplayName("재고가 충분하면 허브 노선이 등록된다")
void createHubRoute_WithSufficientCapacity_Success() {
    // given: 허브와 충분한 재고
    Hub hub = HubFixture.createDefault();
    given(hubRepository.findById(hub.getId())).willReturn(Optional.of(hub));

    // when: 노선 등록 시도
    HubRoute route = hubRouteService.createRoute(hub.getId(), "Busan");

    // then: 성공적으로 등록됨
    assertThat(route.getDestination()).isEqualTo("Busan");
    verify(hubRepository).findById(hub.getId());
}
```

---

## Fixture 사용법

### 개념

**Fixture**란 테스트에서 반복적으로 사용하는 **객체나 상태를 미리 만들어두는 도우미**입니다.

### 사용 목적

**테스트를 읽기 쉬우면서도 중복없이 만들기** 위함입니다.
"항상 쓰는 데이터/상태"를 함수, 클래스, 빌더로 표준화해 둔 것으로, 테스트 코드의 **가독성, 일관성, 유지보수성**을 높입니다.

---

### 작성 위치

```
src/test/java/com/project/support/fixtures/
    ├─ ProductFixture.java
    ├─ OrderFixture.java
    ├─ HubFixture.java
    └─ JsonFixture.java
```

---

### 작성 규칙

| 규칙 | 설명 |
|------|------|
| 도메인별 클래스 분리 | `ProductFixture`, `OrderFixture` 등 |
| static 메서드 제공 | `default()`, `withStock(5)`, `deleted()` |
| 상태 전이 포함 가능 | `completed()`, `canceledBy("user")` 등 |
| 불필요한 로직 금지 | 생성/세팅까지만, 계산 로직 금지 |
| 결정적 데이터 | 랜덤/시간 고정 (UUID, Clock.fixed 등) |

---

### 작성 예시

<details>
<summary><b>간단한 Fixture 작성 예시</b></summary>

```java
// ProductFixture.java
public final class ProductFixture {
    private ProductFixture() {}

    public static Product defaultProduct() {
        return Product.builder()
            .id(UUID.randomUUID())
            .name("기본 상품")
            .price(Money.of(10000))
            .stock(100)
            .build();
    }

    public static Product withStock(int stock) {
        return Product.builder()
            .id(UUID.randomUUID())
            .name("재고 테스트 상품")
            .price(Money.of(10000))
            .stock(stock)
            .build();
    }

    public static Product deleted() {
        var p = defaultProduct();
        p.softDelete("tester");
        return p;
    }
}
```

</details>

<details>
<summary><b>사용 예시</b></summary>

```java
@Test
@DisplayName("재고가 충분하면 주문 생성 성공")
void createOrder_success() {
    // Given
    var product = ProductFixture.withStock(50);

    // When
    var order = Order.create(product.getId(), 10);

    // Then
    assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
}
```

</details>

---

### Fixture 패턴 3가지

#### 1. Object Mother 패턴

<details>
<summary><b>상세 설명 펼치기</b></summary>

**개념:**
항상 쓰는 기본 상태를 정적 메서드로 미리 만들어두는 가장 단순한 패턴입니다.
테스트에서 의미만 드러나게 하고, 중복 생성 코드를 없앱니다.

**사용 상황:**
- 기본 값이 자주 필요한 도메인 (예: Product, User)
- 복잡한 조합이 필요하지 않고, 상태 몇 가지면 충분한 경우

**장점:**
- 간단하고 빠름
- 테스트 본문이 명확해짐 (`ProductFixture.defaultProduct()`)

**주의할 점:**
- 변형 상태가 많아지면 메서드 폭발 → 복잡한 케이스는 Builder 패턴으로 전환

**예시:**
```java
// ProductFixture.java
public final class ProductFixture {
    private ProductFixture() {}

    public static Product defaultProduct() {
        return Product.builder()
            .id(UUID.randomUUID())
            .name("테스트 상품")
            .price(Money.of(10000))
            .stock(100)
            .build();
    }

    public static Product withStock(int stock) {
        return defaultProduct().toBuilder().stock(stock).build();
    }

    public static Product deleted() {
        var product = defaultProduct();
        product.softDelete("tester");
        return product;
    }
}
```

</details>

---

#### 2. Test Data Builder 패턴

<details>
<summary><b>상세 설명 펼치기</b></summary>

**개념:**
Builder 패턴을 응용해 유연한 조합과 재사용을 가능하게 합니다.
특정 속성만 바꾸고 싶을 때 반복된 코드를 없앱니다.

**사용 상황:**
- 테스트마다 일부 필드만 다른 데이터가 필요한 경우
- 예: 재고 수량, 금액, 상태가 자주 바뀌는 Order, Delivery, Product

**장점:**
- 필드 조합이 많을 때 유연함
- 기존 테스트 수정 시 영향 적음

**주의할 점:**
- Builder를 남용하면 Fixture 자체가 커질 수 있음 → 공통값은 Object Mother, 변형값만 Builder로

**예시:**
```java
// ProductTestBuilder.java
public class ProductTestBuilder {
    private UUID id = UUID.randomUUID();
    private String name = "기본 상품";
    private Money price = Money.of(10000);
    private int stock = 100;

    public static ProductTestBuilder aProduct() {
        return new ProductTestBuilder();
    }

    public ProductTestBuilder stock(int stock) {
        this.stock = stock;
        return this;
    }

    public ProductTestBuilder price(int won) {
        this.price = Money.of(won);
        return this;
    }

    public Product build() {
        return Product.builder()
            .id(id)
            .name(name)
            .price(price)
            .stock(stock)
            .build();
    }
}
```

**사용:**
```java
var lowStockProduct = ProductTestBuilder.aProduct().stock(3).build();
var expensiveProduct = ProductTestBuilder.aProduct().price(50000).build();
```

</details>

---

#### 3. Fixture + ParameterizedTest 패턴

<details>
<summary><b>상세 설명 펼치기</b></summary>

**개념:**
Fixture로 데이터 생성 + `@ParameterizedTest`로 다양한 입력을 함께 사용합니다.
하나의 테스트로 경계값, 조합 케이스를 자동화합니다.

**사용 상황:**
- 수량, 금액, 거리 등 경계 조건이 명확한 비즈니스 로직
- 예: 수량은 1 이상이어야 한다, 거리 200Km 초과 시 경유지 추가

**장점:**
- 중복 테스트 제거
- 빠른 회귀 검증 (한 번 실행으로 여러 케이스 확인)

**주의할 점:**
- 너무 많은 입력값을 넣으면 테스트 속도 감소 → 핵심 케이스만 남길 것

**예시:**
```java
@ParameterizedTest
@ValueSource(ints = {0, -1, -10})
@DisplayName("주문 수량이 1 미만이면 예외 발생")
void createOrder_invalidQuantity_throwsException(int quantity) {
    // Given
    var product = ProductFixture.defaultProduct();

    // When & Then
    assertThatThrownBy(() -> Order.create(product.getId(), quantity))
        .isInstanceOf(InvalidQuantityException.class)
        .hasMessageContaining("수량은 1 이상이어야 합니다");
}
```

</details>

---

### Fixture 작성 체크리스트

- [ ] 테스트 본문에 `new`나 `builder()`가 남아있다면 Fixture로 이동했는가?
- [ ] Fixture 이름만 읽어도 의도가 보이는가?
- [ ] 랜덤/시간 값은 항상 고정되어 있는가?
- [ ] Fixture가 비즈니스 로직을 포함하지 않는가?

---

### 참고 자료

> **Fixture Monkey**라는 도구도 있습니다.
> 자세한 내용: https://naver.github.io/fixture-monkey/v1-0-0-kor/docs/introduction/overview/

---

## Mock 사용 가이드

> 이 문서에서의 Mock은 행동 검증(Behavior Verification)을 위한 Mock과 상태 검증(State Verification)을 위한 Mock(Stub)을 분리하여 사용 가이드를 안내합니다.

---

### 1. 행동 검증(Behavior Verification)을 위한 Mock

<details>
<summary><b>사용 목적 및 범위</b></summary>

#### 사용 목적
- 서비스 간 호출 관계를 검증하는 도구로 사용합니다.
- **"무엇을 반환했나"**가 아니라 **"무엇을 했나"**를 중점으로 고민하고 적용합니다.
- "행동 검증"에 사용합니다.

#### 사용 가능 범위
- **외부 의존성** (네트워크, DB, 파일, 타사 API)에 대한 의존성
- **협력 기반 테스트** (Service, Controller)
- **호출 여부와 순서를 검증**할 때
  - 예시: 결제 성공 후 이메일이 발송되었는가 → "행위 검증 중심"

#### 사용 불가 범위
- 통합 테스트나 E2E 테스트 (실제 시스템과의 상호작용을 검증해야 할 때)
- Mock은 **"테스트 환경을 통제하기 위한 도구"**로, 로직 자체를 검증하는 경우에는 실제 객체가 더 신뢰됩니다.

</details>

<details>
<summary><b>사용 방식 및 예시</b></summary>

#### 사용 방식
- **생성 방식**: 행위 검증 (`mock()`)
- **검증 방식**: `verify()`

#### 예시

```java
@ExtendWith(MockitoExtension.class)
class MockExampleTest {

    // [Mock] 외부 의존성을 모두 가짜로 만든다.
    // 목적: 실제 DB, 결제 API, 이메일 전송을 호출하지 않고
    // "UserService가 이 객체들과 올바르게 협력하는가"만 검증한다.
    @Mock private UserRepository repo;
    @Mock private PaymentGateway payment;
    @Mock private EmailSender email;

    // @InjectMocks는 위의 Mock 객체들을 UserService 생성자에 자동 주입한다.
    // 즉, service 내부에서 사용하는 의존성들이 모두 가짜로 대체된 상태다.
    @InjectMocks private UserService service;

    @Test
    void mock_example_verifyBehavior() {
        // [Stub 설정] Mock 객체이지만, 특정 결과를 반환하도록 미리 지정할 수 있다.
        // 즉, 테스트를 제어하기 위한 최소한의 Stub 역할도 병행한다.
        given(repo.existsByEmail(any())).willReturn(false);
        given(payment.charge(any(), anyInt())).willReturn(true);
        given(repo.save(any())).willAnswer(inv -> inv.getArgument(0));

        // [테스트 실행] 실제 비즈니스 로직 실행
        service.signUp("mock@test.com", 1000);

        // [행위 검증] 이제부터가 Mock의 진짜 역할이다.
        // UserService가 내부적으로 외부 의존성들과 올바르게 "협력했는가"를 검증한다.
        // 즉, 결과값보다 "무엇을 호출했는가"가 중요하다.

        // 회원 존재 여부 확인이 호출되었는가
        verify(repo).existsByEmail("mock@test.com");
        // 결제 요청이 실제로 호출되었는가
        verify(payment).charge("mock@test.com", 1000);
        // 회원가입 완료 후 이메일이 발송되었는가
        verify(email).sendWelcome("mock@test.com");
    }
}
```

</details>

---

### 2. 상태 검증(State Verification)을 위한 Mock

<details>
<summary><b>사용 목적 및 범위</b></summary>

#### 사용 목적
- 특정 상황을 재현하기 위해 예상되는 반환값을 설정합니다.
- 테스트 대상의 상태와 결과값을 검증합니다.
- **"무엇을 했나"**가 아닌 **"무엇을 반환했나"**를 중심으로 봅니다.

#### 사용 가능 범위
- 외부 의존성(DB, API, 파일 등)의 결과를 고정시키고 싶을 때
- 예외 상황을 재현할 때

#### 사용 불가 범위
- **도메인의 비즈니스 규칙 검증**
  - "대출 한도가 1000만 원을 넘을 수 없다" 같은 규칙 → **Stub 없이 실제 객체를 써서** 도메인 단위 테스트로 검증하기
- 통합 테스트나 E2E 테스트
- **특정 세부 구현 메서드에 붙어서 사용하는 경우**
  - `given(repository.findUserById(1L))`처럼 **내부 메서드 호출 방식**에 의존하는 Stub은 리팩터링 시 쉽게 깨짐 → 사용 지양

</details>

<details>
<summary><b>사용 방식 및 예시</b></summary>

#### 사용 방식
- **생성 방식**: 반환값 지정 (`mock()`)
- **반환값 지정**: `given(...).willReturn(...)`

#### 예시

```java
@Test
void stub_example_paymentFail() {
    // mock()으로 만든 객체는 "행위 검증(Mock)"에도, "상태 제어(Stub)"에도 모두 쓸 수 있습니다.
    // 즉, Mockito의 mock()은 도구일 뿐이고,
    // 그걸 어떻게 쓰느냐(검증용 vs 제어용)에 따라 역할이 달라집니다.

    // [보조 Stub] UserRepository는 DB에 접근하지 않는다.
    // "해당 이메일이 아직 존재하지 않는다"는 상태를 인위적으로 만들어
    // 회원가입 로직이 이 조건을 타도록 강제한다.
    // 즉, 외부 DB 의존성을 제거하고 테스트 환경을 통제하기 위한 보조 Stub이다.
    UserRepository repo = mock(UserRepository.class);
    given(repo.existsByEmail(any())).willReturn(false);

    // [핵심 Stub] PaymentGateway는 실제 결제 API를 호출하지 않는다.
    // 대신, "결제가 실패했다(false)"는 결과를 강제로 반환하도록 설정했다.
    // 이렇게 외부 결제 시스템의 실패 상황을 재현함으로써
    // UserService의 예외 처리 분기를 검증할 수 있다.
    // 즉, 테스트 목적의 중심이 되는 Stub이다.
    PaymentGateway stubPayment = mock(PaymentGateway.class);
    given(stubPayment.charge(any(), anyInt())).willReturn(false); // 실패 상황 강제

    // [Dummy] EmailSender는 단순히 생성자 인자 맞추기용으로만 쓰인다.
    // 테스트에서 호출되어서는 안 되며, 호출돼도 아무 일도 하지 않는다.
    EmailSender dummyEmail = email -> {};

    // [테스트 대상] UserService 내부에서는 위의 Stub과 Dummy가 사용된다.
    // 여기서 검증하려는 핵심은 "결제 실패 시 예외가 발생하는가"이다.
    UserService service = new UserService(repo, stubPayment, dummyEmail);

    // [검증] 결제가 실패하도록 Stub에서 결과를 강제로 조작했으므로,
    // UserService는 결제 실패 예외(RuntimeException)를 던져야 한다.
    assertThatThrownBy(() -> service.signUp("fail@test.com", 1000))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("결제 실패");
}
```

</details>

---

## 요약

### 핵심 원칙

1. **테스트 이름은 명확하게** - 무엇을, 어떤 상황에서, 어떤 결과를 기대하는지 드러나게
2. **Fixture로 중복 제거** - 반복되는 테스트 데이터는 Fixture로 관리
3. **Mock은 목적에 맞게** - 행동 검증용과 상태 검증용을 구분해서 사용
4. **Given-When-Then 패턴 준수** - 테스트의 가독성과 유지보수성 향상

---

**작성일**: 2025-11-06
**버전**: 1.0