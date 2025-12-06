package org.sparta.user.infrastructure.scheduler;

import lombok.RequiredArgsConstructor;
import org.sparta.user.application.service.PointService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PointReservationScheduler {

    private final PointService pointService;

    // 1분마다 만료 예약 체크
    @Scheduled(fixedRate = 60000)
    public void checkExpiredReservations() {
        pointService.expireReservations();
    }
}