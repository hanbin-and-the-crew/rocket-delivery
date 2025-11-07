package org.sparta.hub.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sparta.hub.domain.entity.Hub;
import org.sparta.hub.domain.model.HubStatus;
import org.sparta.hub.domain.repository.HubRepository;
import org.sparta.hub.exception.DuplicateHubNameException;
import org.sparta.hub.presentation.dto.request.HubCreateRequest;

import org.sparta.hub.presentation.dto.request.HubUpdateRequest;
import org.sparta.hub.presentation.dto.response.HubCreateResponse;
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
        // given
        HubCreateRequest request = new HubCreateRequest(
                "경기 북부 허브",
                "경기도 고양시 덕양구 무슨로 123",
                37.6532,
                126.8321
        );

        // when
        HubCreateResponse response = hubService.createHub(request);

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
        HubCreateRequest request = new HubCreateRequest(
                "서울 허브",
                "서울특별시 강남구 무슨로 45",
                37.55,
                127.01
        );
        hubService.createHub(request);

        // when & then
        assertThatThrownBy(() -> hubService.createHub(request))
                .isInstanceOf(DuplicateHubNameException.class)
                .hasMessageContaining("이미 존재하는 허브명");
    }



    @Test
    @DisplayName("허브 정보를 수정하면 DB에 변경이 반영된다")
    void updateHub_success() {
        // given
        HubCreateRequest createRequest = new HubCreateRequest(
                "서울 허브",
                "서울시 강남구 테헤란로 100",
                37.55,
                127.03
        );
        HubCreateResponse created = hubService.createHub(createRequest);

        HubUpdateRequest updateRequest = new HubUpdateRequest(
                created.hubId(),
                "서울 허브",
                "서울시 송파구 수정로 77",
                37.51,
                127.10,
                HubStatus.ACTIVE
        );

        // when
        hubService.updateHub(updateRequest);

        // then
        Hub updated = hubRepository.findById(created.hubId())
                .orElseThrow(() -> new IllegalStateException("허브가 존재하지 않음"));

        assertThat(updated.getAddress()).isEqualTo("서울시 송파구 수정로 77");
        assertThat(updated.getLatitude()).isEqualTo(37.51);
        assertThat(updated.getLongitude()).isEqualTo(127.10);
        assertThat(updated.getStatus()).isEqualTo(HubStatus.ACTIVE);
    }

}
