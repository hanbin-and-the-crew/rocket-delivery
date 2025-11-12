package org.sparta.hub.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sparta.common.api.ApiControllerAdvice;
import org.sparta.common.event.EventPublisher;
import org.sparta.hub.domain.entity.Hub;
import org.sparta.hub.domain.repository.HubRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "app.eventpublisher.enabled=false"  // ✅ EventPublisher 비활성화
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(ApiControllerAdvice.class)
class HubControllerIntegrationTest {

    @MockBean  // ✅ EventPublisher Mock 주입
    private EventPublisher eventPublisher;

    @Autowired private MockMvc mockMvc;
    @Autowired private HubRepository hubRepository;
    @Autowired private ObjectMapper objectMapper;

    private Hub savedHub;

    @BeforeEach
    void setUp() {
        hubRepository.deleteAll();
        Hub hub = Hub.create("서울 허브", "서울특별시 강남구 테헤란로 123", 37.55, 127.03);
        savedHub = hubRepository.saveAndFlush(hub);

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();
    }

    @Test
    @DisplayName("운영자 목록 조회 - 기본 ALL, status 파라미터로 필터링")
    void admin_list_all_and_filtering() throws Exception {
        hubRepository.deleteAll();
        var a1 = hubRepository.save(Hub.create("A1", "addr", 1.0, 1.0));
        var i1 = Hub.create("I1", "addr", 2.0, 2.0);
        i1.markDeleted("tester");
        hubRepository.save(i1);

        // ALL
        mockMvc.perform(get("/api/admin/hubs").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.length()").value(2));

        // ACTIVE
        mockMvc.perform(get("/api/admin/hubs").param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value("A1"));

        // INACTIVE
        mockMvc.perform(get("/api/admin/hubs").param("status", "INACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value("I1"));
    }

    @Test
    @DisplayName("운영자 단건 조회 - INACTIVE도 200 반환")
    void admin_get_inactive_ok() throws Exception {
        var i1 = Hub.create("I2", "addr", 2.0, 2.0);
        i1.markDeleted("tester");
        Hub saved = hubRepository.save(i1);

        mockMvc.perform(get("/api/admin/hubs/{hubId}", saved.getHubId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.name").value("I2"));
    }

    @Test
    @DisplayName("운영자 삭제(Soft) 후 - 사용자에겐 404, 운영자 목록 INACTIVE에 반영")
    void admin_delete_soft_then_userCantSee() throws Exception {
        var hub = hubRepository.save(Hub.create("DEL", "addr", 3.0, 3.0));

        // delete
        mockMvc.perform(delete("/api/admin/hubs/{hubId}", hub.getHubId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.result").value("SUCCESS"));

        // 사용자 단건 조회는 404
        mockMvc.perform(get("/api/hubs/{hubId}", hub.getHubId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.meta.errorCode").value("common:not_found"));

        // 운영자 INACTIVE 목록에 포함
        mockMvc.perform(get("/api/admin/hubs").param("status", "INACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].name").value(hasItem("DEL")));
    }

    @Test
    @DisplayName("허브 수정 요청이 성공하면 200과 변경된 허브 정보를 반환한다")
    void updateHub_success() throws Exception {
        Hub saved = hubRepository.save(Hub.create(
                "서울 허브", "서울시 강남구 테헤란로 123", 37.55, 127.03
        ));

        String requestBody = """
        {
            "hubId": "%s",
            "name": "서울 허브",
            "address": "서울시 송파구 중대로 77",
            "latitude": 37.51,
            "longitude": 127.10,
            "status": "ACTIVE"
        }
        """.formatted(saved.getHubId());

        mockMvc.perform(put("/api/hubs/{hubId}", saved.getHubId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.hubId").value(saved.getHubId().toString()))
                .andExpect(jsonPath("$.data.address").value("서울시 송파구 중대로 77"))
                .andExpect(jsonPath("$.data.latitude").value(37.51))
                .andExpect(jsonPath("$.data.longitude").value(127.10));
    }

    @Test
    @DisplayName("허브 삭제 성공 - INACTIVE로 변경")
    void deleteHub_success() throws Exception {
        mockMvc.perform(delete("/api/hubs/{hubId}", savedHub.getHubId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));
    }

    @Test
    @DisplayName("존재하지 않는 허브 삭제 시 404 반환")
    void deleteHub_notFound() throws Exception {
        mockMvc.perform(delete("/api/hubs/{hubId}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.meta.result").value("FAIL"))
                .andExpect(jsonPath("$.meta.errorCode").value("common:not_found"))
                .andExpect(jsonPath("$.meta.message").value("Hub not found"));
    }

    @Test
    @DisplayName("이미 삭제된 허브 삭제 시 409 반환")
    void deleteHub_alreadyDeleted() throws Exception {
        savedHub.markDeleted("tester");
        hubRepository.save(savedHub);

        mockMvc.perform(delete("/api/hubs/{hubId}", savedHub.getHubId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.meta.result").value("FAIL"))
                .andExpect(jsonPath("$.meta.errorCode").value("common:conflict"))
                .andExpect(jsonPath("$.meta.message").value("이미 삭제된 허브입니다"));
    }
}
