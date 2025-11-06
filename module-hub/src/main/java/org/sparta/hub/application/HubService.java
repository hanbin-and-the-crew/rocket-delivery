package org.sparta.hub.application;

import lombok.RequiredArgsConstructor;
import org.sparta.common.error.BusinessException;
import org.sparta.common.error.CommonErrorType;
import org.sparta.hub.domain.entity.Hub;
import org.sparta.hub.domain.model.HubStatus;
import org.sparta.hub.domain.repository.HubRepository;
import org.sparta.hub.exception.DuplicateHubNameException;
import org.sparta.hub.exception.HubNotFoundException;
import org.sparta.hub.presentation.dto.request.HubCreateRequest;
import org.sparta.hub.presentation.dto.response.HubCreateResponse;
import org.sparta.hub.presentation.dto.response.HubResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Hub 도메인의 핵심 비즈니스 로직 서비스 계층
 * - 허브 생성, 전체 조회, 단건 조회
 * - 트랜잭션 관리 및 도메인 정책 수행
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HubService {

    private final HubRepository hubRepository;

    /**
     * 허브 생성
     * 중복 이름 검증 후 신규 허브를 등록한다.
     */
    @Transactional
    public HubCreateResponse createHub(HubCreateRequest request) {
        // 중복 검증
        if (hubRepository.existsByName(request.name())) {
            throw new DuplicateHubNameException(request.name());
        }

        // 허브 생성
        Hub hub = Hub.create(
                request.name(),
                request.address(),
                request.latitude(),
                request.longitude()
        );

        Hub saved = hubRepository.save(hub);

        // 응답 DTO 변환
        return HubCreateResponse.from(saved);
    }

    /**
     * 전체 허브 조회
     */
    public List<HubResponse> getAllHubs() {
        List<Hub> hubs = hubRepository.findAll();
        return hubs.stream()
                .map(HubResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 허브 단건 조회
     */
    public HubResponse getHubById(UUID hubId) {
        Hub hub = hubRepository.findById(hubId)
                .orElseThrow(() -> new HubNotFoundException(hubId));

        return HubResponse.from(hub);
    }
}
