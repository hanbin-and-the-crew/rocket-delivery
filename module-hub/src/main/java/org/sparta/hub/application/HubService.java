package org.sparta.hub.application;

import lombok.RequiredArgsConstructor;
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
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 허브 도메인의 핵심 비즈니스 로직 서비스 계층
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HubService {

    private final HubRepository hubRepository;

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
        return HubCreateResponse.from(saved);
    }

    /**
     * 허브 전체 조회 - getAllHubs
     */
    public List<HubResponse> getAllHubs() {
        return hubRepository.findAll()
                .stream()
                .map(HubResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 허브 단건 조회 - getHubById
     */
    public HubResponse getHubById(UUID hubId) {
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
        return HubResponse.from(hub);
    }

    /**
     * 허브 삭제(비활성화) - deleteHub
     */
    @Transactional
    public void deleteHub(UUID hubId) {
        Hub hub = hubRepository.findById(hubId)
                .orElseThrow(() -> new HubNotFoundException(hubId));

        // 이미 삭제된 허브인 경우
        if (hub.getStatus() == HubStatus.INACTIVE) {
            throw new AlreadyDeletedHubException();
        }

        // 허브 비활성화 처리 (Soft Delete)
        hub.markDeleted("system"); // 삭제자 임시 설정 (추후 인증 사용자로 대체 가능)
    }


}
