package org.sparta.hub.application;

import lombok.RequiredArgsConstructor;
import org.sparta.hub.domain.entity.Hub;
import org.sparta.hub.domain.repository.HubRepository;
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

    public List<HubResponse> getAllHubs() {
        return hubRepository.findAll()
                .stream()
                .map(HubResponse::from)
                .collect(Collectors.toList());
    }

    public HubResponse getHubById(UUID hubId) {
        Hub hub = hubRepository.findById(hubId)
                .orElseThrow(() -> new HubNotFoundException(hubId));
        return HubResponse.from(hub);
    }


    @Transactional
    public void updateHub(HubUpdateRequest request) {
        Hub hub = hubRepository.findById(request.hubId())
                .orElseThrow(() -> new HubNotFoundException(request.hubId()));

        hub.update(request.address(), request.latitude(), request.longitude(), request.status());
        // Dirty Checking + Optimistic Lock 자동 반영
    }

}
