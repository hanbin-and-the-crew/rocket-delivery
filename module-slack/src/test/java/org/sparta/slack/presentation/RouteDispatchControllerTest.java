package org.sparta.slack.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sparta.slack.application.route.dto.DailyDispatchResult;
import org.sparta.slack.application.service.route.CompanyDeliveryRouteCommandService;
import org.sparta.slack.application.service.route.CompanyDeliveryRouteQueryService;
import org.sparta.slack.application.service.route.DailyRouteDispatchService;
import org.sparta.slack.domain.entity.CompanyDeliveryRoute;
import org.sparta.slack.domain.enums.RouteStatus;
import org.sparta.slack.presentation.dto.route.RouteRegistrationRequest;
import org.sparta.slack.presentation.dto.route.RouteStopRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RouteDispatchController.class)
@AutoConfigureMockMvc(addFilters = false)
class RouteDispatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CompanyDeliveryRouteCommandService commandService;

    @MockBean
    private CompanyDeliveryRouteQueryService queryService;

    @MockBean
    private DailyRouteDispatchService dailyRouteDispatchService;

    @Test
    @DisplayName("경로 등록 API는 저장된 경로 정보를 반환한다")
    void registerRoute_ShouldReturnRouteResponse() throws Exception {
        UUID routeId = UUID.randomUUID();
        UUID deliveryId = UUID.randomUUID();
        UUID originHubId = UUID.randomUUID();
        UUID destinationCompanyId = UUID.randomUUID();

        CompanyDeliveryRoute route = mock(CompanyDeliveryRoute.class);
        given(route.getId()).willReturn(routeId);
        given(route.getDeliveryId()).willReturn(deliveryId);
        given(route.getScheduledDate()).willReturn(LocalDate.of(2025, 12, 12));
        given(route.getStatus()).willReturn(RouteStatus.PENDING);
        given(route.getOriginHubName()).willReturn("경기북부 센터");
        given(route.getDestinationCompanyName()).willReturn("해산물월드");
        given(route.getDestinationAddress()).willReturn("부산광역시 사하구 낙동대로 1");
        given(route.getDeliveryManagerName()).willReturn("고길동");
        given(route.getDispatchedAt()).willReturn(LocalDateTime.of(2025, 12, 12, 6, 0));
        given(commandService.register(any())).willReturn(route);

        String payload = objectMapper.writeValueAsString(new RouteRegistrationRequest(
                deliveryId,
                LocalDate.of(2025, 12, 12),
                originHubId,
                "경기북부 센터",
                "경기도 의정부시",
                destinationCompanyId,
                "해산물월드",
                "부산광역시 사하구 낙동대로 1",
                List.of(new RouteStopRequest(
                        deliveryId,
                        "A업체",
                        "경기 성남시",
                        37.4,
                        127.12,
                        1
                ))
        ));

        mockMvc.perform(post("/api/slack/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.routeId").value(routeId.toString()))
                .andExpect(jsonPath("$.originHubName").value("경기북부 센터"))
                .andExpect(jsonPath("$.destinationCompanyName").value("해산물월드"));
    }

    @Test
    @DisplayName("특정 날짜의 경로 목록을 조회할 수 있다")
    void listRoutes_ShouldReturnRoutesForDate() throws Exception {
        CompanyDeliveryRoute route = mock(CompanyDeliveryRoute.class);
        given(route.getId()).willReturn(UUID.randomUUID());
        given(route.getDeliveryId()).willReturn(UUID.randomUUID());
        given(route.getScheduledDate()).willReturn(LocalDate.of(2025, 12, 12));
        given(route.getStatus()).willReturn(RouteStatus.DISPATCHED);
        given(route.getOriginHubName()).willReturn("경기북부 센터");
        given(route.getDestinationCompanyName()).willReturn("해산물월드");
        given(route.getDestinationAddress()).willReturn("부산");
        given(route.getDeliveryManagerName()).willReturn("고길동");
        given(route.getDispatchedAt()).willReturn(LocalDateTime.of(2025, 12, 12, 8, 0));
        given(queryService.findByDate(LocalDate.of(2025, 12, 12))).willReturn(List.of(route));

        mockMvc.perform(get("/api/slack/routes")
                        .param("date", "2025-12-12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("DISPATCHED"))
                .andExpect(jsonPath("$[0].originHubName").value("경기북부 센터"));
    }

    @Test
    @DisplayName("루트 발송 API는 결과 목록을 반환한다")
    void dispatchRoutes_ShouldReturnDispatchResults() throws Exception {
        UUID routeId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        given(dailyRouteDispatchService.dispatch(any(LocalDate.class)))
                .willReturn(List.of(DailyDispatchResult.success(routeId, messageId)));

        mockMvc.perform(post("/api/slack/routes/dispatch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"date\":\"2025-12-12\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].success").value(true))
                .andExpect(jsonPath("$[0].routeId").value(routeId.toString()));
    }
}
