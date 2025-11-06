package org.sparta.hub.application;

import lombok.RequiredArgsConstructor;
import org.sparta.hub.domain.entity.Hub;
import org.sparta.hub.domain.repository.HubRepository;
import org.sparta.hub.exception.DuplicateHubNameException;
import org.sparta.hub.exception.HubNotFoundException;
import org.sparta.hub.presentation.dto.response.HubCreateResponse;
import org.sparta.hub.presentation.dto.response.HubResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class HubService {

    private final HubRepository hubRepository;

    public HubCreateResponse createHub(HubCreateCommand command) {

        if (hubRepository.existsByName(command.name())) {
            throw new DuplicateHubNameException(command.name());
        }

        Hub hub = Hub.create(
                command.name(),
                command.address(),
                command.latitude(),
                command.longitude()
        );

        Hub saved = hubRepository.save(hub);
        return HubCreateResponse.from(saved);
    }





    @Transactional(readOnly = true)
    public List<HubResponse> findAllHubs() {
        return hubRepository.findAll().stream()
                .map(HubResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public HubResponse findHubById(UUID hubId) {
        Hub hub = hubRepository.findById(hubId)
                .orElseThrow(() -> new HubNotFoundException(hubId));
        return HubResponse.from(hub);
    }





}
