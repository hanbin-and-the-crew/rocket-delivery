package org.sparta.hub.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sparta.hub.domain.entity.Hub;
import org.sparta.hub.domain.repository.HubRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HubControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HubRepository hubRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Hub savedHub;

    @BeforeEach
    void setUp() {
        hubRepository.deleteAll();

        Hub hub = Hub.create(
                "서울 허브",
                "서울특별시 강남구 테헤란로 123",
                37.55,
                127.03
        );
        savedHub = hubRepository.save(hub);
    }

    @Test
    @DisplayName("전체 허브 조회 성공")
    void getAllHubs_success() throws Exception {
        mockMvc.perform(get("/api/hubs")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("서울 허브"))
                .andExpect(jsonPath("$[0].address").value("서울특별시 강남구 테헤란로 123"));
    }

    @Test
    @DisplayName("단건 허브 조회 성공")
    void getHubById_success() throws Exception {
        mockMvc.perform(get("/api/hubs/{hubId}", savedHub.getHubId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("서울 허브"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("존재하지 않는 허브 조회 시 404 반환")
    void getHubById_notFound() throws Exception {
        mockMvc.perform(get("/api/hubs/{hubId}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Hub not found"));
    }
}
