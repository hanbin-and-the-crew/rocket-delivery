package org.sparta.hub.application;

import lombok.RequiredArgsConstructor;
import org.sparta.common.event.EventPublisher;
import org.sparta.common.event.hub.HubCreatedEvent;
import org.sparta.common.event.hub.HubDeletedEvent;
import org.sparta.common.event.hub.HubUpdatedEvent;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;


/**
 * 허브 도메인의 핵심 비즈니스 로직 서비스 계층
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HubService {

    private final HubRepository hubRepository;
    private final EventPublisher eventPublisher;
    private static final String DEFAULT_DELETER = "system";

    /**
     * 허브 생성 기능 - creatHub
     */
    @Transactional
    public HubCreateResponse createHub(HubCreateRequest request) {

        if (hubRepository.existsByName(request.name())) {
            throw new DuplicateHubNameException(request.name());
        }

        Hub hub = Hub.create(
                request.name(),
                request.address(),
                request.latitude(),
                request.longitude()
        );

        Hub saved = hubRepository.save(hub);

        eventPublisher.publishExternal(
                new HubCreatedEvent(saved.getHubId(), saved.getName(), saved.getAddress())
        );

        return HubCreateResponse.from(saved);
    }

    /**
     * 허브 전체 조회 - 사용자용
     *  ACTIVE 상태 허브만 조회되게 허용
     */
    public List<HubResponse> getActiveHubsForUser() {
        return hubRepository.findAllByStatus(HubStatus.ACTIVE).stream()
                .map(HubResponse::from)
                .toList();
    }

    /**
     * 허브 단건 조회 - 사용자용
     *  ACTIVE 상태 허브만 조회되게 허용
     */
    public HubResponse getActiveHubByIdForUser(UUID hubId) {
        Hub hub = hubRepository.findById(hubId)
                .orElseThrow(() -> new HubNotFoundException(hubId));
        if (hub.isDeleted()) {
            // INACTIVE(=소프트 삭제)면 사용자에겐 404 처리
            throw new HubNotFoundException(hubId);
        }
        return HubResponse.from(hub);
    }

    /**
     * 허브 전체 조회 - 운영자용
     *  모든 상태의 허브 조회
     */
    public List<HubResponse> getHubsForAdmin(String statusParam) {
        String normalized = statusParam == null ? "ALL" : statusParam.toUpperCase(Locale.ROOT);
        if ("ALL".equals(normalized)) {
            return hubRepository.findAll().stream().map(HubResponse::from).toList();
        }
        HubStatus status = HubStatus.valueOf(normalized);
        return hubRepository.findAllByStatus(status).stream().map(HubResponse::from).toList();
    }

    /**
     * 허브 단건 조회 - 운영자용
     *  모든 상태의 허브 조회
     */
    public HubResponse getHubByIdForAdmin(UUID hubId) {
        Hub hub = hubRepository.findById(hubId)
                .orElseThrow(() -> new HubNotFoundException(hubId));
        return HubResponse.from(hub);
    }

    /**
     * 허브 수정 - updateHub
     */
    @Transactional
    public HubResponse updateHub(UUID hubId, HubUpdateRequest request) {
        Hub hub = hubRepository.findById(hubId)
                .orElseThrow(() -> new HubNotFoundException(hubId));

        hub.update(request.address(), request.latitude(), request.longitude(), request.status());
        hubRepository.flush();

        eventPublisher.publishExternal(
                HubUpdatedEvent.of(hub.getHubId(), hub.getName(), hub.getAddress())
        );

        return HubResponse.from(hub);
    }

    /**
     * 허브 삭제(비활성화) - deleteHub
     */
    @Transactional
    public HubResponse deleteHub(UUID hubId) {
        Hub hub = hubRepository.findById(hubId)
                .orElseThrow(() -> new HubNotFoundException(hubId));

        if (hub.isDeleted()) throw new AlreadyDeletedHubException();

        hub.markDeleted(DEFAULT_DELETER);
        hubRepository.flush();

        eventPublisher.publishExternal(
                new HubDeletedEvent(hub.getHubId())
        );

        return HubResponse.from(hub);
    }


}
