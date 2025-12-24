package org.sparta.user.e2e;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sparta.common.event.EventPublisher;
import org.sparta.user.UserApplication;
import org.sparta.user.domain.enums.DeliveryManagerRoleEnum;
import org.sparta.user.domain.enums.UserRoleEnum;
import org.sparta.user.infrastructure.SecurityDisabledConfig;
import org.sparta.user.presentation.dto.request.UserRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Testcontainers;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import org.sparta.user.domain.repository.UserRepository;

import java.util.UUID;
import java.util.List;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {UserApplication.class, SecurityDisabledConfig.class})
@Testcontainers
@ActiveProfiles("test")
class UserApiIntegrationTest {

    @LocalServerPort
    private int port;

    @MockitoBean
    private EventPublisher eventPublisher;

    @Autowired
    private UserRepository userRepository;

    private static final UUID hubId = UUID.fromString("20000000-0000-0000-0000-000000000002");

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        userRepository.deleteAll(); // 테스트 격리
    }

    @Test
    @DisplayName("회원가입 → 승인 → 목록 확인 E2E 테스트")
    void signupApproveAndListUsers_FullFlow() {

        // 1. 회원가입
        UserRequest.SignUpUser signupRequest = new UserRequest.SignUpUser(
                "e2eUser", "pw123!", "slack01", "홍길동",
                "01011112222", "e2e@test.com", UserRoleEnum.MASTER, DeliveryManagerRoleEnum.COMPANY, hubId
        );

        String userId = given()
                .contentType(ContentType.JSON)
                .body(signupRequest)
            .when()
                .post("/api/users/signup")
            .then()
                .statusCode(200)
                .extract()
                .path("data.userId");

        UUID uuid = UUID.fromString(userId);

        // 2. 회원 승인
        given()
                .when()
                .patch("/api/users/bos/{userId}/approve", uuid)
                .then()
                .statusCode(200)
                .extract()
                .path("data");

        // 3. 사용자 목록 조회
        Response response = given()
                .contentType(ContentType.JSON)
                .when()
                .get("/api/users/bos")
                .then()
                .statusCode(200)
                .extract()
                .response();

        String responseBody = response.getBody().asString();
        List<String> userIds = response.jsonPath().getList("data.userId");

        if (userIds.isEmpty() || userIds.get(0) == null) {
            userIds = response.jsonPath().getList("data[0].userId");
        }
        if (userIds.isEmpty() || userIds.get(0) == null) {
            List<Object> allData = response.jsonPath().getList("data");
        }

        // 4. 승인된 회원이 목록에 있는지 검증
        assertThat(userIds)
                .as("User IDs should contain the registered user")
                .isNotEmpty()
                .contains(uuid.toString());
    }
}