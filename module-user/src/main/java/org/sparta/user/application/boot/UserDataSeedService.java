package org.sparta.user.application.boot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.user.domain.entity.User;
import org.sparta.user.domain.enums.DeliveryManagerRoleEnum;
import org.sparta.user.domain.enums.UserRoleEnum;
import org.sparta.user.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDataSeedService {

    private final UserRepository userRepository;

    @Transactional
    public void seedUsers() {
        log.info("유저 데이터 생성 중...");

        // 고정 UUID 샘플 (허브 모듈과 연결 없음)
        UUID hubSeoul = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID hubBusan = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID hubDaegu = UUID.fromString("33333333-3333-3333-3333-333333333333");
        UUID hubIncheon = UUID.fromString("44444444-4444-4444-4444-444444444444");
        UUID hubGwangju = UUID.fromString("55555555-5555-5555-5555-555555555555");

        userRepository.save(User.create("masterAdmin", "1234", "slack_master", "최대철",
                "010-1111-1111", "master@example.com", UserRoleEnum.MASTER, hubSeoul));

        userRepository.save(User.create("user0001", "2345", "slack_seoul", "김서울",
                "010-2222-2222", "seoul_hub@example.com", UserRoleEnum.HUB_MANAGER, hubSeoul));

        userRepository.save(User.create("user0002", "1234", "slack_busan", "박부산",
                "010-3333-3333", "busan_hub@example.com", UserRoleEnum.HUB_MANAGER, hubBusan));

        userRepository.save(User.create("user0003", "2345", "slack_dh1", "이배송",
                "010-4444-4444", "dh1@example.com", UserRoleEnum.DELIVERY_MANAGER, null, DeliveryManagerRoleEnum.HUB));

        userRepository.save(User.create("user0004", "1234", "slack_dh2", "최배송",
                "010-5555-5555", "dh2@example.com", UserRoleEnum.DELIVERY_MANAGER, hubBusan, DeliveryManagerRoleEnum.COMPANY));

        userRepository.save(User.create("user0005", "2345", "slack_dc1", "김업체",
                "010-6666-6666", "dc1@example.com", UserRoleEnum.DELIVERY_MANAGER, hubDaegu, DeliveryManagerRoleEnum.COMPANY));

        userRepository.save(User.create("user0006", "1234", "slack_dc2", "박업체",
                "010-7777-7777", "dc2@example.com", UserRoleEnum.DELIVERY_MANAGER, hubIncheon, DeliveryManagerRoleEnum.COMPANY));

        userRepository.save(User.create("user0007", "2345", "slack_cm1", "이업체장",
                "010-8888-8888", "cm1@example.com", UserRoleEnum.COMPANY_MANAGER, hubGwangju));

        userRepository.save(User.create("user0008", "1234", "slack_cm2", "최업체장",
                "010-9999-9999", "cm2@example.com", UserRoleEnum.COMPANY_MANAGER, hubSeoul));

        userRepository.save(User.create("user0009", "2345", "slack_u1", "홍길동",
                "010-1010-1010", "user1@example.com", UserRoleEnum.DELIVERY_MANAGER, null, DeliveryManagerRoleEnum.HUB));

        userRepository.save(User.create("user0010", "1234", "slack_u2", "임꺽정",
                "010-2020-2020", "user2@example.com", UserRoleEnum.DELIVERY_MANAGER, hubDaegu, DeliveryManagerRoleEnum.COMPANY));

        userRepository.save(User.create("user0011", "2345", "slack_u3", "장보고",
                "010-3030-3030", "user3@example.com", UserRoleEnum.HUB_MANAGER, hubIncheon));

        userRepository.save(User.create("user0012", "1234", "slack_u4", "신사임당",
                "010-4040-4040", "user4@example.com", UserRoleEnum.COMPANY_MANAGER, hubGwangju));

        userRepository.save(User.create("user0013", "2435", "slack_u5", "이순신",
                "010-5050-5050", "user5@example.com", UserRoleEnum.DELIVERY_MANAGER, null, DeliveryManagerRoleEnum.HUB));

        userRepository.save(User.create("user0014", "1234", "slack_u6", "강감찬",
                "010-6060-6060", "user6@example.com", UserRoleEnum.DELIVERY_MANAGER, null, DeliveryManagerRoleEnum.HUB));

        log.info("유저 데이터 생성 완료 (총 15개)");
    }
}
