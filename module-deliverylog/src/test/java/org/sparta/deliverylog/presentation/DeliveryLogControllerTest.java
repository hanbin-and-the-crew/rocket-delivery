package org.sparta.deliverylog.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sparta.deliverylog.application.dto.DeliveryLogRequest;
import org.sparta.deliverylog.application.service.DeliveryLogService;
import org.sparta.deliverylog.domain.entity.DeliveryLog;
import org.sparta.deliverylog.infrastructure.repository.DeliveryLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@ComponentScan(basePackages = "org.sparta.deliverylog.config")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)   // Spring 컨텍스트를 테스트 후 깨끗이 해제
@EmbeddedKafka(partitions = 1, topics = {"delivery-log.created"})   // kafka 실제로 띄우지 않고 test
@DisplayName("DeliveryLogController 통합 테스트")
class DeliveryLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DeliveryLogRepository deliveryLogRepository;

    @Autowired
    private DeliveryLogService deliveryLogService;

    @Test
    @DisplayName("[통합] 배송 경로 생성 성공")
    void createDeliveryLog_Integration_Success() throws Exception {
        // given
        DeliveryLogRequest.Create request = new DeliveryLogRequest.Create(
                UUID.randomUUID(),
                1,
                UUID.randomUUID(),
                UUID.randomUUID(),
                10.0,
                30
        );

        // when & then
        mockMvc.perform(post("/delivery-logs")
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Role", "MASTER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.deliveryId").value(request.deliveryId().toString()))
                .andExpect(jsonPath("$.data.hubSequence").value(request.hubSequence()))
                .andExpect(jsonPath("$.data.expectedDistance").value(request.expectedDistance()))
                .andExpect(jsonPath("$.data.expectedTime").value(request.expectedTime()));
    }

    @Test
    @DisplayName("[통합] 배송 담당자 배정 성공")
    void assignDeliveryMan_Integration_Success() throws Exception {
        // given
        DeliveryLog deliveryLog = createAndSaveTestDeliveryLog();
        UUID deliveryManId = UUID.randomUUID();

        DeliveryLogRequest.Assign request = new DeliveryLogRequest.Assign(deliveryManId);

        // when & then
        mockMvc.perform(patch("/delivery-logs/{deliveryLogId}/assign", deliveryLog.getDeliveryLogId())
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Role", "HUB_MANAGER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.deliveryManId").value(deliveryManId.toString()));
    }

    @Test
    @DisplayName("[통합] 배송 시작 성공")
    void startDelivery_Integration_Success() throws Exception {
        // given
        DeliveryLog deliveryLog = createAndSaveTestDeliveryLog();
        deliveryLog.assignDeliveryMan(UUID.randomUUID());
        deliveryLogRepository.save(deliveryLog);

        // when & then
        mockMvc.perform(patch("/delivery-logs/{deliveryLogId}/start", deliveryLog.getDeliveryLogId())
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Role", "DELIVERY_MAN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("MOVING"));
    }

    @Test
    @DisplayName("[통합] 배송 완료 성공")
    void completeDelivery_Integration_Success() throws Exception {
        // given
        DeliveryLog deliveryLog = createAndSaveTestDeliveryLog();
        deliveryLog.assignDeliveryMan(UUID.randomUUID());
        deliveryLog.startDelivery();
        deliveryLogRepository.save(deliveryLog);

        DeliveryLogRequest.Complete request = new DeliveryLogRequest.Complete(11.0, 32);

        // when & then
        mockMvc.perform(patch("/delivery-logs/{deliveryLogId}/complete", deliveryLog.getDeliveryLogId())
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Role", "DELIVERY_MAN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.actualDistance").value(request.actualDistance()))
                .andExpect(jsonPath("$.data.actualTime").value(request.actualTime()));
    }

    @Test
    @DisplayName("[통합] 배송 경로 단건 조회 성공")
    void getDeliveryLog_Integration_Success() throws Exception {
        // given
        DeliveryLog deliveryLog = createAndSaveTestDeliveryLog();

        // when & then
        mockMvc.perform(get("/delivery-logs/{deliveryLogId}", deliveryLog.getDeliveryLogId())
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Role", "MASTER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.deliveryLogId").value(deliveryLog.getDeliveryLogId().toString()));
    }

    @Test
    @DisplayName("[통합] 배송 ID로 전체 경로 조회 성공")
    void getDeliveryLogsByDeliveryId_Integration_Success() throws Exception {
        // given
        UUID deliveryId = UUID.randomUUID();
        createAndSaveTestDeliveryLogWithDeliveryId(deliveryId, 1);
        createAndSaveTestDeliveryLogWithDeliveryId(deliveryId, 2);
        createAndSaveTestDeliveryLogWithDeliveryId(deliveryId, 3);

        // when & then
        mockMvc.perform(get("/delivery-logs/delivery/{deliveryId}", deliveryId)
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Role", "MASTER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(3));
    }

    @Test
    @DisplayName("[통합] 전체 목록 조회 성공")
    void getAllDeliveryLogs_Integration_Success() throws Exception {
        // given
        createAndSaveTestDeliveryLog();
        createAndSaveTestDeliveryLog();

        // when & then
        mockMvc.perform(get("/delivery-logs")
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Role", "MASTER")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @DisplayName("[통합] 배송 경로 취소 성공")
    void cancelDeliveryLog_Integration_Success() throws Exception {
        // given
        DeliveryLog deliveryLog = createAndSaveTestDeliveryLog();

        // when & then
        mockMvc.perform(patch("/delivery-logs/{deliveryLogId}/cancel", deliveryLog.getDeliveryLogId())
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Role", "HUB_MANAGER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // verify
        DeliveryLog canceled = deliveryLogRepository.findById(deliveryLog.getDeliveryLogId()).get();
        assertThat(canceled.getDeliveryStatus().name()).isEqualTo("CANCELED");
    }

    @Test
    @DisplayName("[통합] 배송 경로 삭제 성공")
    void deleteDeliveryLog_Integration_Success() throws Exception {
        // given
        DeliveryLog deliveryLog = createAndSaveTestDeliveryLog();

        // when & then
        mockMvc.perform(delete("/delivery-logs/{deliveryLogId}", deliveryLog.getDeliveryLogId())
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Role", "MASTER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // verify
        assertThat(deliveryLogRepository.findById(deliveryLog.getDeliveryLogId())).isEmpty();
    }

    @Test
    @DisplayName("[통합] 배송 경로 조회 실패 - 존재하지 않음")
    void getDeliveryLog_Integration_Fail_NotFound() throws Exception {
        // given
        UUID nonExistentId = UUID.randomUUID();

        // when & then
        mockMvc.perform(get("/delivery-logs/{deliveryLogId}", nonExistentId)
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Role", "MASTER"))
                .andExpect(status().isNotFound());
    }

    // ========== Helper Methods ==========

    private DeliveryLog createAndSaveTestDeliveryLog() {
        DeliveryLog deliveryLog = DeliveryLog.create(
                UUID.randomUUID(),
                1,
                UUID.randomUUID(),
                UUID.randomUUID(),
                10.0,
                30
        );
        return deliveryLogRepository.save(deliveryLog);
    }

    private DeliveryLog createAndSaveTestDeliveryLogWithDeliveryId(UUID deliveryId, Integer sequence) {
        DeliveryLog deliveryLog = DeliveryLog.create(
                deliveryId,
                sequence,
                UUID.randomUUID(),
                UUID.randomUUID(),
                10.0,
                30
        );
        return deliveryLogRepository.save(deliveryLog);
    }
}
