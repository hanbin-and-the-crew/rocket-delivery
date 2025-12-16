package org.sparta.coupon;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Testcontainers 공통 설정
 * - H2 인메모리 DB 사용 (CI 환경 안정성)
 * - Redis 컨테이너 제공
 * - 모든 통합 테스트는 이 클래스를 상속받아 사용
 */
@SpringBootTest(properties = {
        "spring.kafka.bootstrap-servers=localhost:9999",  // Kafka 비활성화
        "eureka.client.enabled=false",  // Eureka 비활성화
        "spring.jpa.hibernate.ddl-auto=create-drop"  // 테스트마다 스키마 재생성
})
@Testcontainers
public abstract class TestContainersConfig {

    @Container
    protected static final GenericContainer<?> redis = new GenericContainer<>("redis:6.2.11")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // H2 인메모리 DB 설정 (PostgreSQL 호환 모드)
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");

        // Redis 설정
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }
}