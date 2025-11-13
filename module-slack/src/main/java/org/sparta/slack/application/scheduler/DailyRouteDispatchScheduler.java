package org.sparta.slack.application.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.slack.application.service.route.DailyRouteDispatchService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyRouteDispatchScheduler {

    private final DailyRouteDispatchService dailyRouteDispatchService;

    @Scheduled(cron = "${slack.notification.daily-dispatch-cron:0 0 6 * * *}", zone = "Asia/Seoul")
    public void run() {
        LocalDate targetDate = LocalDate.now(ZoneId.of("Asia/Seoul"));
        log.info("일일 경로 Slack 알림 스케줄 시작 - {}", targetDate);
        dailyRouteDispatchService.dispatch(targetDate);
    }
}
