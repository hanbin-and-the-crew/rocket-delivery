Slack / 알람 관리

- AI 연동 기능
    - [ ]  발송 허브 담당자에게 배송 예상 시간 알림 처리
      (물류를 요청한 업체에서 원하는 시간에 도착할 수 있도록 언제 발송을 해야할 지, 발송 허브 쪽 담당자가 확인하는 용도의 정보 제공)
        - [ ]  생성형 AI의 API로 최종 발송 시한을 포함한 정보를 생성해주세요.

          요청 시 AI에 전달할 데이터들

            - 상품 및 수량 정보 등
            - 주문 요청 사항 (납기일자 및 시간 등)
            - 발송지, 경유지, 도착지 정보
            - 배송 담당자 근무시간 ( 09 - 18 )

          AI 응답에 포함 되어야 하는 데이터들

            - 요청 정보를 모두 고려하여 이 때까진 보내야 납기에 맞출 수 있다 하는 마지막 시점. 즉, 최종 발송 시한

          ※ 생성형 AI에 요청 시 관련 정보를 추가로 정리해 달라고 하셔도 되고, 최종 발송 시한 정보만 가져와도 됩니다.

        - [ ]  주문이 발생한 시점에 슬랙을 통해서 발송 허브 담당자에게 생성된 메시지를 보내서 알려주세요.
          (슬랙 API KEY를 여러개 쓰기 어려울 경우, 하나의 API KEY를 이용하여 처리)

      > **전달 메시지 예시**

      주문 번호 : 1
      ****주문자 정보 : 김말숙 / msk@seafood.world
      주문 시간 : 2025-12-08 10:00:00
      ****상품 정보 : 마른 오징어 50박스
      요청 사항 : 12월 12일 3시까지는 보내주세요!
      발송지 : 경기 북부 센터
      경유지 : 대전광역시 센터, 부산광역시 센터
      도착지 : 부산시 사하구 낙동대로 1번길 1 해산물월드
      배송담당자 : 고길동 / kdk@sparta.world

      위 내용을 기반으로 도출된 최종 발송 시한은 12월 10일 오전 9시 입니다.
      >

- 슬랙 메시지 관리
    - [ ]  슬랙 메시지를 저장하는 엔티티를 생성합니다. 필수 정보는 다음과 같습니다.
        - 수신 ID, 메시지, 발송 시간등
    - [ ]  슬랙 앱을 생성하고 API를 통해 메시지 발송 기능을 구현합니다.
    - [ ]  권한 관리는 아래와 같습니다.
        - 로그인한 모든 사용자 및 내부 시스템은 발송 가능

      |  | 생성 | 수정 | 삭제 | 조회 및 검색 |
              | --- | --- | --- | --- | --- |
      | `마스터 관리자` | O | O | O | O |
      | `허브 관리자`  | O | X  | X | X |
      | `배송 담당자` | O | X | X | X |
      | `업체 담당자` | O  | X | X | X |

담당자 : 채호정


- **AI Aram Context**


    | 구분 | 설명 |
    | --- | --- |
    | **핵심 역할** | 주문 발생 시점에 AI를 통해 “언제 발송해야 납기에 맞출 수 있는가”를 계산하고, 이를 Slack으로 전달 |
    | **하위 도메인 분류** | Supporting Subdomain (물류 핵심 업무를 보조하는 기능) |
    | **주요 책임** | 1. AI 요청 및 응답 관리 2. Slack 메시지 생성 및 발송 3. 발송 이력 저장 4. AI 오류 및 예외 보정 |
    | **Upstream** | `Order Context` (주문 발생 이벤트를 트리거로 수신) |
    | **Downstream** | `Slack Context` (메시지 발송 처리 및 로그 저장) |
    | **외부 통합** | `Gemini API` (생성형 AI 호출), `Slack API` (알림 발송) |
    
    ---
    
    ### Bounded Context
    
    1. AI Planning : 최종 발송 시한 계산(생성형 AI 연동, 규칙/보정)만 책임
    2. Notification : Slack 메시지 생성/발송/저장 플랫폼화.
    
    ---
    
    ### Notification
    
    책임
    
    - 채널 불문 메시지 **템플릿 렌더링**, **발송**, **저장/리포트**
    - **Slack 1개 키**로 멀티 수신자 발송
    - 향후 Email/SMS/Webhook 추가 확장점 고정
    
    ### [Message Aggregate]
    
    | 이름 | 타입 | 유형 | 의미 |
    | --- | --- | --- | --- |
    | id | pk | entity(root) | 메시지 고유 식별자 |
    | channel | Enum | value | 메시지 발송 채널 (SLACK / EMAIL / SMS / WEBHOOK 등) |
    | recipient | Recipient | VO | 수신자 정보 (slackId, email 등) |
    | templateCode | String | value | 사용할 메시지 템플릿 코드 |
    | payload | JSON | value | 템플릿 렌더링에 필요한 변수 데이터 |
    | status | Enum | value | PENDING / SENT / FAILED |
    | sentAt | LocalDateTime | value | 실제 발송 시각 |
    | deliveryResult | String | VO | 발송 실패 시 에러 코드 |
    | slackDetail | SlackMessageDetail | entity(optional) | Slack 전용 상세 메시지 (receiverSlackId, messageBody 등) |
    | createdAt | LocalDateTime | BaseEntity | 생성시각 |
    | createdBy | String | BaseEntity | 생성자 |
    | updatedAt | LocalDataTime | BaseEntity | 수정 시각 |
    | updatedBy | String | BaseEntity | 수정자 |
    | deletedAt | LocalDateTime | BaseEntity | 논리 삭제 시각 |
    | deletedBy | String | BaseEntity | 논리 삭제 수행자 |
    
    ---
    
    ### [Template Aggregate] /DB seed
    
    | 이름  | 타입 | 유형 | 의미 |
    | --- | --- | --- | --- |
    | id(templateCode) | pk | entity(root) | 템플릿의 고유 식별자 |
    | format | Enum | value | 템플릿 포맷 |
    | content | String | value | 실제 메시지 본문 |
    | channel | Enum | value | 해당 템플릿이 적용될 채널 (SLACK) |
    | createdAt | LocalDateTime | BaseEntity | 생성시각 |
    | createdBy | String | BaseEntity | 생성자 |
    | updatedAt | LocalDataTime | BaseEntity | 수정 시각 |
    | updatedBy | String | BaseEntity | 수정자 |
    | deletedAt | LocalDateTime | BaseEntity | 논리 삭제 시각 |
    | deletedBy | String | BaseEntity | 논리 삭제 수행자 |
    
    ---
    
    ### AI Planning
    
    책임 
    
    - 입력(상품/수량, 납기, 발송/경유/도착지, 근무시간 09–18)으로 **최종 발송 시한** 산출
    - 생성형 AI 호출/보정·Fallback·재시도/감사로그
    - **결과 이벤트 발행**: `AIResponseReceivedEvent`
    
    ### [Planning Aggregate]
    
    주문 기반 AI 계산 요청부터 결과 확정까지 단일 트랜잭션 경계로 관리
    
    | 이름  | 타입 | 유형 | 의미 |
    | --- | --- | --- | --- |
    | id | pk | entity(root) |  |
    | orderId |  | value | 동일 `orderId`에 **동시에 하나의 OPEN 요청만** 존재(Idempotency). |
    | status | Enum | value | REQUESTED / COMPLETED / FAILED |
    | payloadSnapshot |  | VO | 주문 서비스로부터 전달받은 **상품, 수량, 납기, 경로, 담당자 근무시간 등**을“AI 요청에 필요한 입력” 형태로 구조화해서하나의 **불변 VO** 로 저장 |
    | requestedAt | LocalDateTime | value | 요청 생성 시각 |
    | response | PlanningResponse | entity | planningRequestId,
    deadline,
    aiRawOutput,
    reason |
    | createdAt | LocalDateTime | BaseEntity | 생성시각 |
    | createdBy | String | BaseEntity | 생성자 |
    | updatedAt | LocalDataTime | BaseEntity | 수정 시각 |
    | updatedBy | String | BaseEntity | 수정자 |
    | deletedAt | LocalDateTime | BaseEntity | 논리 삭제 시각 |
    | deletedBy | String | BaseEntity | 논리 삭제 수행자 |
    
    ### [**PlanningResult**] Value Object (VO)
    
    외부(다른 Context)로 전달( 이벤트의 payload로 사용대상)
이름 	타입	유형	의미
id	pk	entity(root)
deadline	LocalDateTime	VO: Deadline	최종 발송 시한
routeSummary	String	value	AI가 계산한 경유지 요약 정보
reason	String	value	발송 시한 결정 사유 (AI 판단 설명)



- [ ]  매일 업체 배송담당자에게 아침 6시 슬랙 알림을 발송해주세요.
    - [ ]  업체 배송담당자가 당일 방문해야 하는 주소들의 위경도 값을 AI를 통해, 배송 순서를 지정해주세요.
    - [ ]  결과 경유지 순서를 사용하여 네이버 경로 API의 waypoints 파라미터를 사용하여 결과를 받아주세요.
        - [ ]  네이버 Map API 중 **Directions 5 API** 연동을 구현해주세요.(https://api.ncloud-docs.com/docs/ai-naver-mapsdirections-driving)
        - [ ]  경로 API는 위도와 경도 값으로 출발지와 도착지를 입력합니다. 다른 API를 사용하여 이름을 통해 위도 경도를 입력할 수 있지만 google 맵에서 검색하면 주소에 위경도 값이 나옵니다.

      ![image.png](https://prod-files-secure.s3.us-west-2.amazonaws.com/83c75a39-3aba-4ba4-a792-7aefe4b07895/9df5fb14-f829-43f7-bd0d-6de467389a92/image.png)


    - [ ]  해당 방문 순서를 포함한 경로 및 시간 데이터를 포함하여 AI를 통해 메시지를 생성하여 슬랙으로 발송해주세요.
    - [ ]  구현과 테스트 편의성을 위해 발송 시각을 설정파일에서 관리하고 수정할 수 있게 해주세요.
    - [ ]  업체 배송경로 기록 엔티티를 추가해주세요. 엔티티는 배송ID, 출발허브ID, 수령업체, 예상거리, 예상소요시간, 실제거리, 실제 소요시간, 현재상태(업체 이동중, 배송완료 등), 배송 담당자ID, 배송순서 정도를 포함해주세요. 추가적으로 필요하다고 판단되는 필드가 있다면, 자유롭게 추가해주셔도 됩니다.
- [ ]  배송 담당자 배정 로직을 자유롭게 구현해주세요.
    - [ ]  허브 배송 담당자, 업체 배송 담당자가 효율적으로 배송을 완수하기 위한 구현을 해주세요.
    - [ ]  제한사항은 없습니다. 필요한 로직과 정책을 총동원 해주세요!
    - [ ]  예시) 거리를 기반으로 한 효율적인 담당자 배정