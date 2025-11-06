package org.sparta.hub.application;

import lombok.RequiredArgsConstructor;
import org.sparta.hub.domain.entity.Hub;
import org.sparta.hub.domain.repository.HubRepository;
import org.sparta.hub.exception.DuplicateHubNameException;
import org.sparta.hub.presentation.dto.request.HubCreateRequest;
import org.sparta.hub.presentation.dto.response.HubCreateResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 허브 관련 비즈니스 로직을 담당하는 서비스 계층
 *
 * 클래스 레벨 @Transactional:
 *  - 기본적으로 모든 public 메서드가 트랜잭션 경계 내에서 실행됨.
 *  - 읽기 전용 트랜잭션은 메서드에 @Transactional(readOnly = true)로 별도 지정.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class HubService {

    private final HubRepository hubRepository;

    /**
     * 허브 생성 로직
     */
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
}
