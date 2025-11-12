package org.sparta.delivery.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sparta.delivery.application.dto.request.DeliveryRequest;
import org.sparta.delivery.application.dto.response.DeliveryResponse;
import org.sparta.delivery.application.service.DeliveryService;
import org.sparta.delivery.domain.enumeration.DeliveryStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DeliveryController.class)
@DisplayName("DeliveryController 테스트")
class DeliveryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DeliveryService deliveryService;

    // ========== 배송 생성 테스트 ==========

    @Test
    @DisplayName("배송 생성 성공")
    void create_delivery_success() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        DeliveryRequest.Create request = new DeliveryRequest.Create(
                orderId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "서울특별시 강남구 테헤란로 123",
                "홍길동",
                "@홍길동"
        );

        DeliveryResponse.Create response = new DeliveryResponse.Create(
                UUID.randomUUID(),
                orderId,
                DeliveryStatus.HUB_WAITING,
                LocalDateTime.now()
        );

        given(deliveryService.createDelivery(any(DeliveryRequest.Create.class), any(UUID.class)))
                .willReturn(response);

        // when & then
        mockMvc.perform(post("/api/deliveries")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.deliveryId").exists())
                .andExpect(jsonPath("$.data.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.data.deliveryStatus").value("HUB_WAITING"));
    }

    @Test
    @DisplayName("배송 생성 실패 - 필수 파라미터 누락")
    void create_delivery_fail_missing_parameter() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        String invalidRequest = "{}";

        // when & then
        mockMvc.perform(post("/api/deliveries")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    // ========== 배송 목록 조회 테스트 ==========

    @Test
    @DisplayName("배송 목록 조회 성공 - 페이징만 사용")
    void get_all_deliveries_success() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        DeliveryResponse.Summary summary = new DeliveryResponse.Summary(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                DeliveryStatus.HUB_WAITING,
                "홍길동",
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        Pageable pageable = PageRequest.of(0, 10);
        Page<DeliveryResponse.Summary> page = new PageImpl<>(List.of(summary), pageable, 1);

        // ✅ 변경: DeliverySearchCondition 제거, Pageable만 사용
        given(deliveryService.getAllDeliveries(any(Pageable.class)))
                .willReturn(page);

        // when & then
        mockMvc.perform(get("/api/deliveries")
                        .header("X-User-Id", userId.toString())
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].recipientName").value("홍길동"))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.size").value(10));
    }

    @Test
    @DisplayName("배송 목록 조회 성공 - 빈 결과")
    void get_all_deliveries_success_empty() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        Page<DeliveryResponse.Summary> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        given(deliveryService.getAllDeliveries(any(Pageable.class)))
                .willReturn(emptyPage);

        // when & then
        mockMvc.perform(get("/api/deliveries")
                        .header("X-User-Id", userId.toString())
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.content").isEmpty())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    // ========== 배송 상세 조회 테스트 ==========

    @Test
    @DisplayName("배송 상세 조회 성공")
    void get_delivery_success() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        UUID deliveryId = UUID.randomUUID();
        DeliveryResponse.Detail detail = new DeliveryResponse.Detail(
                deliveryId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                DeliveryStatus.HUB_WAITING,
                "서울특별시 강남구 테헤란로 123",
                "홍길동",
                "@홍길동",
                null,
                null,
                LocalDateTime.now(),
                LocalDateTime.now(),
                null
        );

        given(deliveryService.getDelivery(eq(deliveryId), any(UUID.class)))
                .willReturn(detail);

        // when & then
        mockMvc.perform(get("/api/deliveries/{deliveryId}", deliveryId)
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.deliveryId").value(deliveryId.toString()))
                .andExpect(jsonPath("$.data.recipientName").value("홍길동"));
    }

    @Test
    @DisplayName("배송 조회 실패 - User ID 헤더 누락")
    void get_delivery_fail_missing_user_id() throws Exception {
        // given
        UUID deliveryId = UUID.randomUUID();

        // when & then
        mockMvc.perform(get("/api/deliveries/{deliveryId}", deliveryId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    // ========== 배송 상태 변경 테스트 ==========

    @Test
    @DisplayName("배송 상태 변경 성공")
    void update_status_success() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        UUID deliveryId = UUID.randomUUID();
        DeliveryRequest.UpdateStatus request = new DeliveryRequest.UpdateStatus(DeliveryStatus.HUB_MOVING);

        DeliveryResponse.Update response = new DeliveryResponse.Update(
                deliveryId,
                LocalDateTime.now()
        );

        given(deliveryService.updateStatus(
                eq(deliveryId),
                any(DeliveryRequest.UpdateStatus.class),
                any(UUID.class)
        )).willReturn(response);

        // when & then
        mockMvc.perform(patch("/api/deliveries/{deliveryId}/status", deliveryId)
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.deliveryId").value(deliveryId.toString()))
                .andExpect(jsonPath("$.data.updatedAt").exists());
    }

    // ========== 배송 담당자 배정 테스트 ==========

    @Test
    @DisplayName("배송 담당자 배정 성공")
    void assign_delivery_man_success() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        UUID deliveryId = UUID.randomUUID();
        DeliveryRequest.AssignDeliveryMan request = new DeliveryRequest.AssignDeliveryMan(
                UUID.randomUUID(),
                UUID.randomUUID()
        );

        DeliveryResponse.Update response = new DeliveryResponse.Update(
                deliveryId,
                LocalDateTime.now()
        );

        given(deliveryService.assignDeliveryMan(
                eq(deliveryId),
                any(DeliveryRequest.AssignDeliveryMan.class),
                any(UUID.class)
        )).willReturn(response);

        // when & then
        mockMvc.perform(patch("/api/deliveries/{deliveryId}/manager", deliveryId)
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.deliveryId").value(deliveryId.toString()));
    }

    // ========== 배송 삭제 테스트 ==========

    @Test
    @DisplayName("배송 삭제 성공")
    void delete_delivery_success() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        UUID deliveryId = UUID.randomUUID();
        DeliveryResponse.Delete response = new DeliveryResponse.Delete(
                deliveryId,
                LocalDateTime.now()
        );

        given(deliveryService.deleteDelivery(eq(deliveryId), any(UUID.class)))
                .willReturn(response);

        // when & then
        mockMvc.perform(delete("/api/deliveries/{deliveryId}", deliveryId)
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.deliveryId").value(deliveryId.toString()))
                .andExpect(jsonPath("$.data.deletedAt").exists());
    }
}
