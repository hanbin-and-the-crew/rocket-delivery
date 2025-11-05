package org.sparta.hub.application;

import lombok.RequiredArgsConstructor;
import org.sparta.hub.domain.entity.Hub;
import org.sparta.hub.domain.repository.HubRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class HubService {

    private final HubRepository hubRepository;

    public HubResponse createHub(HubCreateRequest request) {
        Hub hub = Hub.create(
                request.name(),
                request.address(),
                request.latitude(),
                request.longitude()
        );
        Hub saved = hubRepository.save(hub);
        return HubResponse.from(saved);
    }
}
