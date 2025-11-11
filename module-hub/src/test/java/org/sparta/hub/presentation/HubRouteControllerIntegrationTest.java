package org.sparta.hub.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sparta.hub.domain.entity.HubRoute;
import org.sparta.hub.domain.model.HubRouteStatus;
import org.sparta.hub.domain.repository.HubRouteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("HubRouteController 통합 테스트")
class HubRouteControllerIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private HubRouteRepository hubRouteRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("POST /api/hub-routes - 생성 성공")
    void createRoute_success() throws Exception {
        String body = """
            {
              "sourceHubId": "%s",
              "targetHubId": "%s",
              "distance": 150,
              "duration": 120
            }
            """.formatted(UUID.randomUUID(), UUID.randomUUID());

        mvc.perform(post("/api/hub-routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.meta.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.distance").value(150))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("GET /api/hub-routes/{id} - 단건 조회 성공")
    void getRoute_success() throws Exception {
        HubRoute route = hubRouteRepository.save(
                HubRoute.builder()
                        .sourceHubId(UUID.randomUUID())
                        .targetHubId(UUID.randomUUID())
                        .distance(200)
                        .duration(180)
                        .status(HubRouteStatus.ACTIVE)
                        .build()
        );

        mvc.perform(get("/api/hub-routes/{id}", route.getRouteId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.routeId").value(route.getRouteId().toString()));
    }

    @Test
    @DisplayName("PUT /api/hub-routes/{id} - 수정 성공")
    void updateRoute_success() throws Exception {
        HubRoute route = hubRouteRepository.save(
                HubRoute.builder()
                        .sourceHubId(UUID.randomUUID())
                        .targetHubId(UUID.randomUUID())
                        .distance(200)
                        .duration(180)
                        .status(HubRouteStatus.ACTIVE)
                        .build()
        );

        String update = """
            {
              "distance": 400,
              "duration": 250
            }
            """;

        mvc.perform(put("/api/hub-routes/{id}", route.getRouteId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(update))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.distance").value(400))
                .andExpect(jsonPath("$.data.duration").value(250));
    }

    @Test
    @DisplayName("DELETE /api/hub-routes/{id} - 비활성화 성공")
    void deleteRoute_success() throws Exception {
        HubRoute route = hubRouteRepository.save(
                HubRoute.builder()
                        .sourceHubId(UUID.randomUUID())
                        .targetHubId(UUID.randomUUID())
                        .distance(120)
                        .duration(80)
                        .status(HubRouteStatus.ACTIVE)
                        .build()
        );

        mvc.perform(delete("/api/hub-routes/{id}", route.getRouteId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/hub-routes - 활성 목록 조회 성공")
    void getAllActiveRoutes_success() throws Exception {
        hubRouteRepository.save(
                HubRoute.builder()
                        .sourceHubId(UUID.randomUUID())
                        .targetHubId(UUID.randomUUID())
                        .distance(120)
                        .duration(100)
                        .status(HubRouteStatus.ACTIVE)
                        .build()
        );

        mvc.perform(get("/api/hub-routes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.length()").value(1));
    }
}
