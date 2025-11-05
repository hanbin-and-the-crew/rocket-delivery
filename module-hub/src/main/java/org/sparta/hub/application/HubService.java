package org.sparta.hub.application;

import lombok.RequiredArgsConstructor;
import org.sparta.hub.domain.entity.Hub;
import org.sparta.hub.domain.repository.HubRepository;
import org.sparta.hub.dto.request.HubCreateRequest;
import org.sparta.hub.dto.response.HubCreateResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class HubService {

    private final HubRepository hubRepository;

    public HubCreateResponse createHub(HubCreateRequest request) {

        if (hubRepository.existsByName(request.name())) {
            throw new IllegalArgumentException("이미 존재하는 허브 이름입니다: " + request.name());
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
}
