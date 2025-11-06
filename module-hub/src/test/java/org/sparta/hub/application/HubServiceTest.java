package org.sparta.hub.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sparta.hub.domain.entity.Hub;
import org.sparta.hub.domain.model.HubStatus;
import org.sparta.hub.domain.repository.HubRepository;
import org.sparta.hub.presentation.dto.response.HubCreateResponse;
import org.sparta.hub.exception.DuplicateHubNameException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(HubService.class)
@ActiveProfiles("test")
class HubServiceTest {

    @Autowired
    private HubRepository hubRepository;

    private HubService hubService;

    @BeforeEach
    void setUp() {
        hubService = new HubService(hubRepository);
    }

    @Test
    @DisplayName("허브를 생성하면 DB에 저장되고, 생성된 허브 정보를 반환한다")
    void createHub_success() {
        // given: 애플리케이션 커맨드 DTO를 사용
        HubCreateCommand cmd = HubCreateCommand.of(
                "경기 북부 허브",
                "경기도 고양시 덕양구 무슨로 123",
                37.6532,
                126.8321
        );

        // when
        HubCreateResponse response = hubService.createHub(cmd);

        // then
        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("경기 북부 허브");
        assertThat(response.status()).isEqualTo("ACTIVE");

        // DB 검증
        Optional<Hub> found = hubRepository.findById(response.hubId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("경기 북부 허브");
        assertThat(found.get().getStatus()).isEqualTo(HubStatus.ACTIVE);
    }

    @Test
    @DisplayName("허브 이름이 중복될 경우 예외를 발생시킨다")
    void createHub_duplicateName_fail() {
        // given
        HubCreateCommand cmd = HubCreateCommand.of(
                "서울 허브",
                "서울특별시 강남구 무슨로 45",
                37.55,
                127.01
        );
        hubService.createHub(cmd);

        // when & then
        assertThatThrownBy(() -> hubService.createHub(cmd))
                .isInstanceOf(DuplicateHubNameException.class)
                .hasMessageContaining("이미 존재하는 허브명");
    }
}
