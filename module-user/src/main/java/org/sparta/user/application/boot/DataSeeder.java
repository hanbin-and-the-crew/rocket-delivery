package org.sparta.user.application.boot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.user.domain.repository.PointRepository;
import org.sparta.user.domain.repository.PointReservationRepository;
import org.sparta.user.domain.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile({"local","dev"})
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserDataSeedService userSeedService;
    private final UserRepository userRepository;
    private final PointDataSeedService pointSeedService;
    private final PointRepository pointRepository;
    private final PointReservationRepository pointReservationRepository;

    @Override
    public void run(String... args) {
        long userCount = userRepository.count();
        log.info("현재 유저 수: {}", userCount);

        if (userCount > 0) {
            log.info("초기 유저 데이터가 이미 존재하여 데이터 시딩을 건너뜁니다.");
        }
        else {
            log.info("User 모듈 초기 데이터 시딩을 시작합니다.");
            userSeedService.seedUsers();
            log.info("User 모듈 초기 데이터 시딩이 완료되었습니다.");
        }

        long pointCount = pointRepository.count();
        log.info("현재 포인트 개수: {}", pointCount);

        if (pointCount > 0) {
            log.info("초기 포인트 데이터가 이미 존재하여 데이터 시딩을 건너뜁니다.");
        }
        else {
            log.info("포인트 모듈 초기 데이터 시딩을 시작합니다.");
            pointSeedService.seedPoints();
            log.info("포인트 모듈 초기 데이터 시딩이 완료되었습니다.");
        }
    }
}

