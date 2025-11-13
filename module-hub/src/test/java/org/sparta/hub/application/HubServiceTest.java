package org.sparta.hub.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sparta.common.event.EventPublisher;
import org.sparta.common.event.hub.HubCreatedEvent;
import org.sparta.hub.domain.entity.Hub;
import org.sparta.hub.domain.model.HubStatus;
import org.sparta.hub.domain.repository.HubRepository;
import org.sparta.hub.exception.AlreadyDeletedHubException;
import org.sparta.hub.exception.DuplicateHubNameException;
import org.sparta.hub.exception.HubNotFoundException;
import org.sparta.hub.presentation.dto.request.HubCreateRequest;
import org.sparta.hub.presentation.dto.request.HubUpdateRequest;
import org.sparta.hub.presentation.dto.response.HubCreateResponse;
import org.sparta.hub.presentation.dto.response.HubResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@DataJpaTest
@Import(HubService.class)
@ActiveProfiles("test")
class HubServiceTest {

    @Autowired
    private HubRepository hubRepository;

    @MockBean
    private EventPublisher eventPublisher;

    @Autowired
    private HubService hubService;

    @BeforeEach
    void init() {
        hubRepository.deleteAll();
    }

    @Test
    @DisplayName("허브 생성 시 DB에 저장되고 이벤트가 발행된다")
    void createHub_success_publishEvent() {
        // given
        HubCreateRequest request = new HubCreateRequest(
                "경기북부허브", "경기도 고양시 덕양구 화정로 12", 37.65, 126.83
        );

        // when
        HubCreateResponse response = hubService.createHub(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("경기북부허브");

        Optional<Hub> found = hubRepository.findById(response.hubId());
        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(HubStatus.ACTIVE);

        verify(eventPublisher, times(1)).publishExternal(any(HubCreatedEvent.class));
    }

    @Test
    @DisplayName("허브 이름이 중복되면 DuplicateHubNameException이 발생한다")
    void createHub_duplicateName_fail() {
        // given
        hubService.createHub(new HubCreateRequest("서울허브", "서울 강남구", 37.5, 127.0));

        // when & then
        assertThatThrownBy(() ->
                hubService.createHub(new HubCreateRequest("서울허브", "서울 강남구", 37.5, 127.0))
        )
                .isInstanceOf(DuplicateHubNameException.class)
                .hasMessageContaining("이미 존재하는 허브명입니다");
    }

/*
    @Test
    @DisplayName("허브 수정 시 주소와 위도/경도가 업데이트된다")
    void updateHub_success() {
        // given
        HubCreateResponse created = hubService.createHub(
                new HubCreateRequest("서울허브", "서울 강남구", 37.5, 127.0)
        );

        // when
        HubResponse updated = hubService.updateHub(created.hubId(),
                new HubUpdateRequest("서울허브", "서울 송파구", 37.51, 127.11, HubStatus.ACTIVE));

        // then
        assertThat(updated.address()).isEqualTo("서울 송파구");
    }
*/

    @Test
    @DisplayName("허브 삭제 시 상태가 INACTIVE로 변경되고, 재삭제 시 예외 발생")
    void deleteHub_success_then_conflict() {
        // given
        HubCreateResponse created = hubService.createHub(
                new HubCreateRequest("삭제테스트허브", "서울 영등포구", 37.52, 126.93)
        );

        // when
        hubService.deleteHub(created.hubId());
        Hub deleted = hubRepository.findById(created.hubId()).orElseThrow();

        // then
        assertThat(deleted.getStatus()).isEqualTo(HubStatus.INACTIVE);

        assertThatThrownBy(() -> hubService.deleteHub(created.hubId()))
                .isInstanceOf(AlreadyDeletedHubException.class)
                .hasMessageContaining("이미 삭제된 허브");
    }

    @Test
    @DisplayName("존재하지 않는 허브 삭제 시 HubNotFoundException 발생")
    void deleteHub_notFound_fail() {
        // expect
        assertThatThrownBy(() -> hubService.deleteHub(UUID.randomUUID()))
                .isInstanceOf(HubNotFoundException.class)
                .hasMessage("Hub not found");
    }

    @Test
    @DisplayName("사용자 조회 시 ACTIVE 허브만 보이고, INACTIVE는 404로 처리된다")
    void user_visibility_rules() {
        // given
        Hub active = hubRepository.save(Hub.create("활성허브", "서울 강남", 1.0, 1.0));
        Hub inactive = Hub.create("비활성허브", "서울 서초", 2.0, 2.0);
        inactive.markDeleted("tester");
        hubRepository.save(inactive);

        // when
        var list = hubService.getActiveHubsForUser();

        // then
        assertThat(list).extracting(HubResponse::name)
                .contains("활성허브")
                .doesNotContain("비활성허브");

        assertThatThrownBy(() -> hubService.getActiveHubByIdForUser(inactive.getHubId()))
                .isInstanceOf(HubNotFoundException.class);
    }
}
