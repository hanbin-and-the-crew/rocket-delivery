package org.sparta.user.application.boot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.user.domain.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile({"local","dev"})
@RequiredArgsConstructor
public class UserDataSeeder implements CommandLineRunner {

    private final UserDataSeedService seedService;
    private final UserRepository userRepository;

    @Override
    public void run(String... args) {
        long userCount = userRepository.count();
        log.info("현재 유저 수: {}", userCount);

        if (userCount > 0) {
            log.info("초기 데이터가 이미 존재하여 데이터 시딩을 건너뜁니다.");
            return;
        }

        log.info("User 모듈 초기 데이터 시딩을 시작합니다.");

        seedService.seedUsers();

        log.info("User 모듈 초기 데이터 시딩이 완료되었습니다.");
    }

}

