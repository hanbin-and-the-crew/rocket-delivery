package org.sparta.slack.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sparta.slack.application.dto.DailyDispatchResult;
import org.sparta.slack.domain.entity.CompanyDeliveryRoute;
import org.sparta.slack.domain.repository.CompanyDeliveryRouteRepository;
import org.sparta.slack.domain.vo.RoutePlanningResult;
import org.sparta.slack.domain.vo.RouteStopSnapshot;
import org.sparta.slack.application.service.AiRoutePlanner;
import org.sparta.slack.application.service.DeliveryAssignmentService;
import org.sparta.slack.application.service.SlackDirectMessageSender;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DailyRouteDispatchServiceTest {

    @Mock
    private CompanyDeliveryRouteRepository routeRepository;

    @Mock
    private DeliveryAssignmentService assignmentService;

    @Mock
    private AiRoutePlanner aiRoutePlanner;

    @Mock
    private SlackDirectMessageSender slackDirectMessageSender;

    @InjectMocks
    private DailyRouteDispatchService dailyRouteDispatchService;

    private CompanyDeliveryRoute route;
    private RoutePlanningResult planningResult;

    @BeforeEach
    void setUp() {
        List<RouteStopSnapshot> stops = List.of(
                RouteStopSnapshot.builder()
                        .deliveryId(UUID.randomUUID())
                        .label("허브")
                        .address("서울특별시 중구")
                        .sequence(0)
                        .latitude(37.56)
                        .longitude(126.97)
                        .build(),
                RouteStopSnapshot.builder()
                        .deliveryId(UUID.randomUUID())
                        .label("업체A")
                        .address("부산광역시 사하구")
                        .sequence(1)
                        .latitude(35.10)
                        .longitude(129.03)
                        .build()
        );

        route = CompanyDeliveryRoute.create(
                UUID.randomUUID(),
                LocalDate.now(),
                UUID.randomUUID(),
                "경기 북부 허브",
                "경기도 고양시",
                UUID.randomUUID(),
                "해산물 월드",
                "부산 사하구 낙동대로",
                stops
        );
        route.assignManager(UUID.randomUUID(), "고길동", "U123456", 1);

        planningResult = new RoutePlanningResult(
                stops,
                120_000L,
                180,
                "허브 → 업체A (120km)",
                "거리 기반 최적화",
                null,
                "[]"
        );
    }

    @Test
    @DisplayName("일일 경로를 계산하고 Slack으로 발송한다")
    void dispatch_ShouldPlanAndSendSlack() {
        when(routeRepository.findAllByScheduledDateAndStatusIn(any(), any()))
                .thenReturn(List.of(route));
        when(aiRoutePlanner.plan(route)).thenReturn(planningResult);
        UUID messageId = UUID.randomUUID();
        when(slackDirectMessageSender.send(any(), any(), any(), any())).thenReturn(messageId);

        List<DailyDispatchResult> results = dailyRouteDispatchService.dispatch(LocalDate.now());

        assertThat(results).hasSize(1);
        assertThat(results.get(0).success()).isTrue();
        verify(routeRepository, times(2)).save(route);
        verify(slackDirectMessageSender).send(
                eq("U123456"),
                eq("ROUTE_DAILY_SUMMARY"),
                any(),
                eq("허브 → 업체A (120km)")
        );
    }
}
