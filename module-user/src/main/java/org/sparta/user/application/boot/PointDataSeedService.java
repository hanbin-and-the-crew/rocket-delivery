package org.sparta.user.application.boot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.common.error.BusinessException;
import org.sparta.user.domain.entity.Point;
import org.sparta.user.domain.entity.PointReservation;
import org.sparta.user.domain.enums.PointStatus;
import org.sparta.user.domain.enums.ReservationStatus;
import org.sparta.user.domain.error.UserErrorType;
import org.sparta.user.domain.repository.PointRepository;
import org.sparta.user.domain.repository.PointReservationRepository;
import org.sparta.user.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PointDataSeedService {

    private final PointRepository pointRepository;
    private final UserRepository userRepository;
    private final PointReservationRepository pointReservationRepository;;

    @Transactional
    public void seedPoints() {
        log.info("포인트 데이터 생성 중...");

        // user0001 (김서울) 조회
        UUID userId1 = userRepository.findByUserName("user0001")
                .orElseThrow(() -> new BusinessException(UserErrorType.USER_NOT_FOUND))
                .getUserId();

        // user0002 (박부산) 조회
        UUID userId2 = userRepository.findByUserName("user0002")
                .orElseThrow(() -> new BusinessException(UserErrorType.USER_NOT_FOUND))
                .getUserId();

        // user0001 (김서울) - 3개의 포인트 배치
        Point point1 = pointRepository.save(Point.create(
                userId1,
                10000L,
                0L,
                0L,
                LocalDateTime.now().plusDays(365),
                PointStatus.AVAILABLE
        ));

        Point point2 = pointRepository.save(Point.create(
                userId1,
                5000L,
                0L,
                0L,
                LocalDateTime.now().plusDays(180),
                PointStatus.AVAILABLE
        ));

        Point point3 = pointRepository.save(Point.create(
                userId1,
                3000L,
                0L,
                0L,
                LocalDateTime.now().plusDays(90),
                PointStatus.AVAILABLE
        ));

        // user0002 (박부산) - 2개의 포인트 배치
        Point point4 = pointRepository.save(Point.create(
                userId2,
                15000L,
                0L,
                0L,
                LocalDateTime.now().plusDays(365),
                PointStatus.AVAILABLE
        ));

        Point point5 = pointRepository.save(Point.create(
                userId2,
                8000L,
                2000L,
                0L,
                LocalDateTime.now().plusDays(120),
                PointStatus.AVAILABLE
        ));

        log.info("포인트 데이터 생성 완료 (총 5개)");
        log.info("user0001(김서울): 10000 + 5000 + 3000 = 18000 포인트");
        log.info("user0002(박부산): 15000 + 8000(예약 2000) = 23000 포인트");


        // 시나리오 1: user0001의 point1(10000)에서 7000 예약
        UUID orderId1 = UUID.randomUUID();
        pointReservationRepository.save(PointReservation.create(
                point1.getId(),
                orderId1,
                7000L,
                ReservationStatus.RESERVED
        ));

        // 시나리오 2: user0002의 point5(8000, 이미 2000 예약됨)에서 추가로 3000 예약
        UUID orderId2 = UUID.randomUUID();
        pointReservationRepository.save(PointReservation.create(
                point5.getId(),
                orderId2,
                3000L,
                ReservationStatus.RESERVED
        ));

        // 시나리오 3: user0001의 point2(5000)에서 5000 예약 후 결제 완료 상태
        UUID orderId3 = UUID.randomUUID();
        pointReservationRepository.save(PointReservation.create(
                point2.getId(),
                orderId3,
                5000L,
                ReservationStatus.CONFIRMED
        ));

        log.info("PointReservation 데이터 생성 완료 (총 3개)");
        log.info("예약1 (RESERVED): point1(10000) 중 7000 예약 - orderId: {}", orderId1);
        log.info("예약2 (RESERVED): point5(8000) 중 3000 추가 예약 - orderId: {}", orderId2);
        log.info("예약3 (CONFIRMED): point2(5000) 전체 예약 및 결제완료 - orderId: {}", orderId3);
    }
}
