package org.sparta.hub.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sparta.common.api.ApiControllerAdvice;
import org.sparta.hub.domain.entity.Hub;
import org.sparta.hub.domain.repository.HubRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(ApiControllerAdvice.class)
class HubControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private HubRepository hubRepository;
    @Autowired private ObjectMapper objectMapper;
    private Hub savedHub;

    @BeforeEach
    void setUp() {
        hubRepository.deleteAll();
        Hub hub = Hub.create("서울 허브", "서울특별시 강남구 테헤란로 123", 37.55, 127.03);
        savedHub = hubRepository.save(hub);
    }

    /**
     * 허브 조회
     * - 전체 조회
     * - 단건 조회
     * - 미존재 허브 조회 예외
     */
    @Test
    @DisplayName("전체 허브 조회 성공")
    void getAllHubs_success() throws Exception {
        mockMvc.perform(get("/api/hubs")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data[0].name").value("서울 허브"));
    }

    @Test
    @DisplayName("단건 허브 조회 성공")
    void getHubById_success() throws Exception {
        mockMvc.perform(get("/api/hubs/{hubId}", savedHub.getHubId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.name").value("서울 허브"));
    }

    @Test
    @DisplayName("존재하지 않는 허브 조회 시 404 반환")
    void getHubById_notFound() throws Exception {
        mockMvc.perform(get("/api/hubs/{hubId}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.meta.result").value("FAIL"))
                .andExpect(jsonPath("$.meta.errorCode").value("common:not_found"))
                .andExpect(jsonPath("$.meta.message").value("Hub not found"));
    }

    /**
     * 허브 수정
     */
    @Test
    @DisplayName("허브 수정 요청이 성공하면 200과 변경된 허브 정보를 반환한다")
    void updateHub_success() throws Exception {
        // given
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

        // when & then
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
    @DisplayName("허브 삭제 성공 시 200과 ApiResponse.success 반환")
    void deleteHub_success() throws Exception {
        // given
        Hub hub = hubRepository.save(Hub.create(
                "삭제 허브", "서울시 마포구 테스트로 1", 37.56, 126.92
        ));

        // when & then
        mockMvc.perform(delete("/api/hubs/{hubId}", hub.getHubId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("이미 삭제된 허브를 다시 삭제하면 409와 에러 응답 반환")
    void deleteHub_alreadyDeleted_conflict() throws Exception {
        // given
        Hub hub = hubRepository.save(Hub.create(
                "중복 삭제 대상", "서울시 성동구 왕십리로 2", 37.56, 127.04
        ));
        // 1차 삭제
        mockMvc.perform(delete("/api/hubs/{hubId}", hub.getHubId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // 2차 삭제 시도 → 409
        mockMvc.perform(delete("/api/hubs/{hubId}", hub.getHubId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.meta.result").value("FAIL"))
                .andExpect(jsonPath("$.meta.errorCode").value("common:conflict"))
                .andExpect(jsonPath("$.meta.message").value("이미 삭제된 허브입니다"));
    }

    @Test
    @DisplayName("존재하지 않는 허브 삭제 시 404와 에러 응답 반환")
    void deleteHub_notFound() throws Exception {
        mockMvc.perform(delete("/api/hubs/{hubId}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.meta.result").value("FAIL"))
                .andExpect(jsonPath("$.meta.errorCode").value("common:not_found"))
                .andExpect(jsonPath("$.meta.message").value("Hub not found"));
    }
}
