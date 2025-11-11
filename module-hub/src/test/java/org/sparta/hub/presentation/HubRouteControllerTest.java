package org.sparta.hub.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sparta.hub.application.HubRouteService;
import org.sparta.hub.domain.entity.HubRoute;
import org.sparta.hub.domain.model.HubRouteStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("HubRouteController 단위 테스트")
@org.junit.jupiter.api.extension.ExtendWith(MockitoExtension.class)
class HubRouteControllerTest {

    @Mock
    private HubRouteService hubRouteService;

    @InjectMocks
    private HubRouteController hubRouteController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mvc() {
        return MockMvcBuilders.standaloneSetup(hubRouteController).build();
    }

    @Test
    @DisplayName("POST /api/hub-routes - 허브 경로 생성 성공")
    void createRoute_success() throws Exception {
        UUID source = UUID.randomUUID();
        UUID target = UUID.randomUUID();

        HubRoute route = HubRoute.builder()
                .routeId(UUID.randomUUID())
                .sourceHubId(source)
                .targetHubId(target)
                .distance(150)
                .duration(120)
                .status(HubRouteStatus.ACTIVE)
                .build();

        given(hubRouteService.createRoute(any(), any(), anyInt(), anyInt())).willReturn(route);

        String body = """
            {
              "sourceHubId": "%s",
              "targetHubId": "%s",
              "distance": 150,
              "duration": 120
            }
            """.formatted(source, target);

        mvc().perform(post("/api/hub-routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.meta.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("GET /api/hub-routes/{id} - 허브 경로 단건 조회 성공")
    void getRoute_success() throws Exception {
        HubRoute route = HubRoute.builder()
                .routeId(UUID.randomUUID())
                .sourceHubId(UUID.randomUUID())
                .targetHubId(UUID.randomUUID())
                .distance(100)
                .duration(60)
                .status(HubRouteStatus.ACTIVE)
                .build();

        given(hubRouteService.getRoute(any())).willReturn(route);

        mvc().perform(get("/api/hub-routes/{id}", route.getRouteId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.routeId").value(route.getRouteId().toString()));
    }

    @Test
    @DisplayName("PUT /api/hub-routes/{id} - 허브 경로 수정 성공")
    void updateRoute_success() throws Exception {
        HubRoute updated = HubRoute.builder()
                .routeId(UUID.randomUUID())
                .distance(300)
                .duration(200)
                .status(HubRouteStatus.ACTIVE)
                .build();

        given(hubRouteService.updateRoute(any(), anyInt(), anyInt())).willReturn(updated);

        String body = """
            {
              "distance": 300,
              "duration": 200
            }
            """;

        mvc().perform(put("/api/hub-routes/{id}", updated.getRouteId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.distance").value(300))
                .andExpect(jsonPath("$.data.duration").value(200));
    }

    @Test
    @DisplayName("DELETE /api/hub-routes/{id} - 허브 경로 삭제(비활성화) 성공")
    void deleteRoute_success() throws Exception {
        given(hubRouteService.deleteRoute(any(UUID.class)))
                .willReturn(HubRoute.builder()
                        .routeId(UUID.randomUUID())
                        .status(HubRouteStatus.INACTIVE)
                        .build());

        mvc().perform(delete("/api/hub-routes/{id}", UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data").doesNotExist()); 
    }




    @Test
    @DisplayName("GET /api/hub-routes - 활성화된 허브 경로 목록 조회 성공")
    void getAllActiveRoutes_success() throws Exception {
        List<HubRoute> mockRoutes = List.of(
                HubRoute.builder()
                        .routeId(UUID.randomUUID())
                        .sourceHubId(UUID.randomUUID())
                        .targetHubId(UUID.randomUUID())
                        .distance(120)
                        .duration(90)
                        .status(HubRouteStatus.ACTIVE)
                        .build()
        );

        given(hubRouteService.getAllActiveRoutes()).willReturn(mockRoutes);

        mvc().perform(get("/api/hub-routes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.length()").value(1));
    }
}
