//package org.sparta.delivery.integration;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.sparta.delivery.application.dto.request.DeliveryRequest;
//import org.sparta.delivery.domain.entity.Delivery;
//import org.sparta.delivery.domain.enumeration.DeliveryStatus;
//import org.sparta.delivery.infrastructure.repository.DeliveryJpaRepository;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.http.MediaType;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.UUID;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//@SpringBootTest
//@AutoConfigureMockMvc
//@Transactional
//@ActiveProfiles("test")
//@DisplayName("Delivery 통합 테스트")
//class DeliveryIntegrationTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    @Autowired
//    private DeliveryJpaRepository deliveryRepository;
//
//    private UUID userId;
//
//    @BeforeEach
//    void setUp() {
//        userId = UUID.randomUUID();
//        deliveryRepository.deleteAll();
//    }
//
//    @Test
//    @DisplayName("배송 생성 -> 조회 -> 수정 -> 삭제 (전체 플로우)")
//    void delivery_full_lifecycle() throws Exception {
//        // 1. 배송 생성
//        DeliveryRequest.Create createRequest = new DeliveryRequest.Create(
//                UUID.randomUUID(),
//                UUID.randomUUID(),
//                UUID.randomUUID(),
//                "서울특별시 강남구 테헤란로 123",
//                "홍길동",
//                "@홍길동"
//        );
//
//        String createResponse = mockMvc.perform(post("/api/deliveries")
//                        .header("X-User-Id", userId.toString())
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(createRequest)))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.meta.result").value("SUCCESS"))
//                .andExpect(jsonPath("$.data.deliveryId").exists())
//                .andReturn()
//                .getResponse()
//                .getContentAsString();
//
//        UUID deliveryId = UUID.fromString(
//                objectMapper.readTree(createResponse)
//                        .get("data")
//                        .get("deliveryId")
//                        .asText()
//        );
//
//        // 2. 배송 상세 조회
//        mockMvc.perform(get("/api/deliveries/{deliveryId}", deliveryId)
//                        .header("X-User-Id", userId.toString())
//                        .contentType(MediaType.APPLICATION_JSON))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.meta.result").value("SUCCESS"))
//                .andExpect(jsonPath("$.data.deliveryId").value(deliveryId.toString()))
//                .andExpect(jsonPath("$.data.recipientName").value("홍길동"));
//
//        // 3. 배송 상태 변경
//        DeliveryRequest.UpdateStatus updateStatusRequest = new DeliveryRequest.UpdateStatus(DeliveryStatus.HUB_MOVING);
//
//        mockMvc.perform(patch("/api/deliveries/{deliveryId}/status", deliveryId)
//                        .header("X-User-Id", userId.toString())
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(updateStatusRequest)))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.meta.result").value("SUCCESS"));
//
//        // 4. 배송 주소 변경
//        DeliveryRequest.UpdateAddress updateAddressRequest = new DeliveryRequest.UpdateAddress(
//                "서울특별시 강남구 역삼로 456"
//        );
//
//        mockMvc.perform(patch("/api/deliveries/{deliveryId}/address", deliveryId)
//                        .header("X-User-Id", userId.toString())
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(updateAddressRequest)))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.meta.result").value("SUCCESS"));
//
//        // 5. 배송 담당자 배정 - ✅ 경로 수정: delivery-man → manager
//        DeliveryRequest.AssignDeliveryMan assignRequest = new DeliveryRequest.AssignDeliveryMan(
//                UUID.randomUUID(),
//                UUID.randomUUID()
//        );
//
//        mockMvc.perform(patch("/api/deliveries/{deliveryId}/manager", deliveryId)
//                        .header("X-User-Id", userId.toString())
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(assignRequest)))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.meta.result").value("SUCCESS"));
//
//        // 6. 배송 삭제
//        mockMvc.perform(delete("/api/deliveries/{deliveryId}", deliveryId)
//                        .header("X-User-Id", userId.toString())
//                        .contentType(MediaType.APPLICATION_JSON))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.meta.result").value("SUCCESS"))
//                .andExpect(jsonPath("$.data.deletedAt").exists());
//
//        // 7. 삭제된 배송은 조회되지 않음
//        Delivery deletedDelivery = deliveryRepository.findById(deliveryId).orElseThrow();
//        assertThat(deletedDelivery.getDeletedAt()).isNotNull();
//    }
//
//    @Test
//    @DisplayName("배송 목록 조회 - 페이징 및 정렬")
//    void get_deliveries_with_paging_and_sorting() throws Exception {
//        // given
//        for (int i = 0; i < 15; i++) {
//            Delivery delivery = Delivery.create(
//                    UUID.randomUUID(),
//                    UUID.randomUUID(),
//                    UUID.randomUUID(),
//                    "서울특별시 강남구 테헤란로 " + i,
//                    "테스트" + i,
//                    "@테스트" + i
//            );
//            deliveryRepository.save(delivery);
//        }
//
//        // when & then
//        mockMvc.perform(get("/api/deliveries")
//                        .header("X-User-Id", userId.toString())
//                        .param("page", "0")
//                        .param("size", "10")
//                        .param("sort", "createdAt,desc")
//                        .contentType(MediaType.APPLICATION_JSON))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.meta.result").value("SUCCESS"))
//                .andExpect(jsonPath("$.data.content").isArray())
//                .andExpect(jsonPath("$.data.content.length()").value(10))
//                .andExpect(jsonPath("$.data.totalElements").value(15))
//                .andExpect(jsonPath("$.data.totalPages").value(2));
//    }
//
////    @Test
////    @DisplayName("배송 목록 조회 - 검색 조건 적용")
////    void get_deliveries_with_search_condition() throws Exception {
////        // given
////        UUID orderId = UUID.randomUUID();
////        Delivery delivery1 = Delivery.create(
////                orderId,
////                UUID.randomUUID(),
////                UUID.randomUUID(),
////                "서울특별시 강남구 테헤란로 123",
////                "홍길동",
////                "@홍길동"
////        );
////        delivery1.hubMoving();
////
////        Delivery delivery2 = Delivery.create(
////                UUID.randomUUID(),
////                UUID.randomUUID(),
////                UUID.randomUUID(),
////                "서울특별시 강남구 테헤란로 456",
////                "김철수",
////                "@김철수"
////        );
////
////        deliveryRepository.saveAll(java.util.List.of(delivery1, delivery2));
////
////        // when & then - 주문 ID로 검색
////        mockMvc.perform(get("/api/deliveries")
////                        .header("X-User-Id", userId.toString())
////                        .param("orderId", orderId.toString())
////                        .contentType(MediaType.APPLICATION_JSON))
////                .andDo(print())
////                .andExpect(status().isOk())
////                .andExpect(jsonPath("$.meta.result").value("SUCCESS"))
////                .andExpect(jsonPath("$.data.content.length()").value(1))
////                .andExpect(jsonPath("$.data.content[0].orderId").value(orderId.toString()));
////
////        // when & then - 배송 상태로 검색
////        mockMvc.perform(get("/api/deliveries")
////                        .header("X-User-Id", userId.toString())
////                        .param("deliveryStatus", "HUB_MOVING")
////                        .contentType(MediaType.APPLICATION_JSON))
////                .andDo(print())
////                .andExpect(status().isOk())
////                .andExpect(jsonPath("$.meta.result").value("SUCCESS"))
////                .andExpect(jsonPath("$.data.content.length()").value(1))
////                .andExpect(jsonPath("$.data.content[0].deliveryStatus").value("HUB_MOVING"));
////
////        // when & then - 수령인 이름으로 검색
////        mockMvc.perform(get("/api/deliveries")
////                        .header("X-User-Id", userId.toString())
////                        .param("recipientName", "홍길")
////                        .contentType(MediaType.APPLICATION_JSON))
////                .andDo(print())
////                .andExpect(status().isOk())
////                .andExpect(jsonPath("$.meta.result").value("SUCCESS"))
////                .andExpect(jsonPath("$.data.content.length()").value(1))
////                .andExpect(jsonPath("$.data.content[0].recipientName").value("홍길동"));
////    }
//
//    @Test
//    @DisplayName("배송 업체 이동 시작 후 완료")
//    void start_company_moving_and_complete() throws Exception {
//        // given
//        Delivery delivery = Delivery.create(
//                UUID.randomUUID(),
//                UUID.randomUUID(),
//                UUID.randomUUID(),
//                "서울특별시 강남구 테헤란로 123",
//                "홍길동",
//                "@홍길동"
//        );
//        Delivery savedDelivery = deliveryRepository.save(delivery);
//        UUID deliveryId = savedDelivery.getId();
//
//        // when & then - 업체 배송 시작
//        UUID companyManId = UUID.randomUUID();
//        DeliveryRequest.StartCompanyMoving startRequest = new DeliveryRequest.StartCompanyMoving(companyManId);
//
//        mockMvc.perform(patch("/api/deliveries/{deliveryId}/start-company-moving", deliveryId)
//                        .header("X-User-Id", userId.toString())
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(startRequest)))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.meta.result").value("SUCCESS"));
//
//        // 상태 확인
//        Delivery updatedDelivery = deliveryRepository.findById(deliveryId).orElseThrow();
//        assertThat(updatedDelivery.getDeliveryStatus()).isEqualTo(DeliveryStatus.COMPANY_MOVING);
//        assertThat(updatedDelivery.getCompanyDeliveryManId()).isEqualTo(companyManId);
//
//        // when & then - 배송 완료
//        mockMvc.perform(patch("/api/deliveries/{deliveryId}/complete", deliveryId)
//                        .header("X-User-Id", userId.toString())
//                        .contentType(MediaType.APPLICATION_JSON))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.meta.result").value("SUCCESS"));
//
//        // 상태 확인
//        Delivery completedDelivery = deliveryRepository.findById(deliveryId).orElseThrow();
//        assertThat(completedDelivery.getDeliveryStatus()).isEqualTo(DeliveryStatus.DELIVERED);
//    }
//
////    @Test
////    @DisplayName("존재하지 않는 배송 조회 시 404 에러")
////    void get_non_existing_delivery_returns_404() throws Exception {
////        // given
////        UUID nonExistingDeliveryId = UUID.randomUUID();
////
////        // when & then
////        mockMvc.perform(get("/api/deliveries/{deliveryId}", nonExistingDeliveryId)
////                        .header("X-User-Id", userId.toString())
////                        .contentType(MediaType.APPLICATION_JSON))
////                .andDo(print())
////                .andExpect(status().isNotFound());
////    }
////
////    @Test
////    @DisplayName("중복된 주문 ID로 배송 생성 시 409 에러")
////    void create_delivery_with_duplicate_order_id_returns_409() throws Exception {
////        // given
////        UUID orderId = UUID.randomUUID();
////        Delivery existingDelivery = Delivery.create(
////                orderId,
////                UUID.randomUUID(),
////                UUID.randomUUID(),
////                "서울특별시 강남구 테헤란로 123",
////                "홍길동",
////                "@홍길동"
////        );
////        deliveryRepository.save(existingDelivery);
////
////        DeliveryRequest.Create request = new DeliveryRequest.Create(
////                orderId,
////                UUID.randomUUID(),
////                UUID.randomUUID(),
////                "서울특별시 강남구 역삼로 456",
////                "김철수",
////                "@김철수"
////        );
////
////        // when & then
////        mockMvc.perform(post("/api/deliveries")
////                        .header("X-User-Id", userId.toString())
////                        .contentType(MediaType.APPLICATION_JSON)
////                        .content(objectMapper.writeValueAsString(request)))
////                .andDo(print())
////                .andExpect(status().isConflict());
////    }
//}
