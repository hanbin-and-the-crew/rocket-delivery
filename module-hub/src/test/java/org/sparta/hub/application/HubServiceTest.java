package org.sparta.hub.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sparta.hub.domain.entity.Hub;
import org.sparta.hub.domain.repository.HubRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("HubService 단위 테스트")
class HubServiceTest {

    @Autowired
    private HubService hubService;

    @Autowired
    private HubRepository hubRepository;

    @Test
    @DisplayName("허브를 생성하면 DB에 저장된다")
    void createHub_success() {
        // given
        HubCreateRequest request = new HubCreateRequest(
                "서울 허브",
                "서울특별시 송파구 문정동",
                37.4876,
                127.1234
        );

        // when
        HubResponse response = hubService.createHub(request);

        // then
        assertThat(response.getName()).isEqualTo("서울 허브");
        assertThat(hubRepository.findAll()).hasSize(1);
    }
}
