package org.sparta.slack.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.sparta.slack.domain.entity.CompanyDeliveryRoute;
import org.sparta.slack.domain.entity.Template;
import org.sparta.slack.domain.enums.Channel;
import org.sparta.slack.domain.enums.RouteStatus;
import org.sparta.slack.domain.enums.TemplateFormat;
import org.sparta.slack.domain.repository.CompanyDeliveryRouteRepository;
import org.sparta.slack.domain.vo.RouteStopSnapshot;
import org.sparta.slack.application.port.out.SlackNotificationSender;
import org.sparta.slack.infrastructure.repository.TemplateJpaRepository;
import org.sparta.slack.user.domain.entity.UserSlackView;
import org.sparta.slack.user.domain.enums.UserRole;
import org.sparta.slack.user.domain.enums.UserStatus;
import org.sparta.slack.user.domain.repository.UserSlackViewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

@SpringBootTest
@ActiveProfiles("test")
@org.springframework.transaction.annotation.Transactional
class RouteDispatchIntegrationTest {

    @Autowired
    private CompanyDeliveryRouteRepository routeRepository;

    @Autowired
    private UserSlackViewRepository userSlackViewRepository;

    @Autowired
    private TemplateJpaRepository templateJpaRepository;

    @Autowired
    private DailyRouteDispatchService dailyRouteDispatchService;

    @MockBean
    private SlackNotificationSender slackNotificationSender;

    @Test
    @DisplayName("샘플 유저/경로 데이터를 기반으로 전체 Slack 발송 플로우를 검증한다")
    void dispatch_ShouldProcessSampleDataEndToEnd() {
        // given
        UUID hubId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        userSlackViewRepository.save(UserSlackView.create(
                userId,
                "route-manager",
                "고길동",
                "U123456789",
                UserRole.DELIVERY_MANAGER,
                UserStatus.APPROVE,
                hubId,
                LocalDateTime.now()
        ));

        templateJpaRepository.save(Template.create(
                "ROUTE_DAILY_SUMMARY",
                TemplateFormat.MARKDOWN,
                "담당자 {{managerName}} / 경로 {{routeSummary}}",
                Channel.SLACK,
                "일일 경로 요약"
        ));

        UUID deliveryId = UUID.randomUUID();
        CompanyDeliveryRoute route = CompanyDeliveryRoute.create(
                deliveryId,
                LocalDate.now(),
                hubId,
                "경기 북부 허브",
                "경기도 고양시 덕양구 통일로 123",
                UUID.randomUUID(),
                "해산물 월드",
                "부산광역시 사하구 낙동대로 1",
                List.of(
                        RouteStopSnapshot.builder()
                                .deliveryId(deliveryId)
                                .label("경기 북부 허브")
                                .address("경기도 고양시 덕양구 통일로 123")
                                .latitude(37.6688)
                                .longitude(126.9784)
                                .sequence(0)
                                .build(),
                        RouteStopSnapshot.builder()
                                .deliveryId(deliveryId)
                                .label("해산물 월드")
                                .address("부산광역시 사하구 낙동대로 1")
                                .latitude(35.074)
                                .longitude(128.958)
                                .sequence(1)
                                .build()
                )
        );
        routeRepository.save(route);

        Mockito.doNothing().when(slackNotificationSender).send(any());

        // when
        dailyRouteDispatchService.dispatch(LocalDate.now());

        // then
        CompanyDeliveryRoute saved = routeRepository.findByDeliveryId(deliveryId).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(RouteStatus.DISPATCHED);
        assertThat(saved.getDeliveryManagerId()).isEqualTo(userId);
        assertThat(saved.getDispatchMessageId()).isNotNull();
        assertThat(saved.getDispatchedAt()).isNotNull();
        assertThat(saved.getRouteSummary()).isNotBlank();

        Mockito.verify(slackNotificationSender, Mockito.atLeastOnce()).send(any());
    }
}
