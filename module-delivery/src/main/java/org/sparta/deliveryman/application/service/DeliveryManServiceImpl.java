package org.sparta.deliveryman.application.service;

import lombok.RequiredArgsConstructor;
import org.sparta.common.error.BusinessException;
import org.sparta.deliveryman.domain.entity.DeliveryMan;
import org.sparta.deliveryman.domain.enumeration.DeliveryManStatus;
import org.sparta.deliveryman.domain.enumeration.DeliveryManType;
import org.sparta.deliveryman.domain.error.DeliveryManErrorType;
import org.sparta.deliveryman.domain.repository.DeliveryManRepository;
import org.sparta.deliveryman.presentation.dto.request.DeliveryManRequest;
import org.sparta.deliveryman.presentation.dto.response.DeliveryManResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryManServiceImpl implements DeliveryManService {

    private final DeliveryManRepository deliveryManRepository;

    // =========================================
    // 1. User 이벤트 기반 생성/수정/삭제
    // =========================================

    // TODO: UserCreateEvent를 수신해서 생성 할 때 조건에 만족될 때만 DeliveryMan생성
    //  ==> UserRole은 DELIVERY_MANAGER / userStatus는 APPROVE인 경우에만 DeliveryMan이 생성되게 해야 됨
    // TODO: user쪽에서 hubId를 null 가능하게 수정완료된 상태. -> 이에 따른 DeliveryManType 판단 로직 수정 (hubId의 null여부에 따라 DeliveryManType 결정)
    // 현재 구현된거는 user에서 null이 되게 한거로 구현 되어있음
    @Override
    @Transactional
    public DeliveryManResponse.Detail create(
            UUID userId,
            UUID hubId,
            String realName,
            String slackId,
            String userRole,
            String userStatus
//            DeliveryManType type
    ) {
        if (userId == null) {
            throw new BusinessException(DeliveryManErrorType.USER_ID_REQUIRED);
        }
//        if (type == null) {
//            throw new BusinessException(DeliveryManErrorType.TYPE_REQUIRED);
//        }

        // 이미 같은 userId로 DeliveryMan이 존재하면 생성 방지 (idempotent 보장 or 중복 방지)
        if (deliveryManRepository.existsByUserIdAndDeletedAtIsNull(userId)) {
            throw new BusinessException(DeliveryManErrorType.NO_CHANGES_TO_UPDATE);
        }
        // DeliveryManType 지정
        DeliveryManType type;
        if (hubId == null) {
            type = DeliveryManType.HUB;
        } else {
            type = DeliveryManType.COMPANY;
        }

        int sequence = calculateNextSequence(type, hubId);

        DeliveryMan deliveryMan;
        if (type == DeliveryManType.HUB) {
            // HUB 타입: hubId는 무조건 null이어야 함
            if (hubId != null) {
                throw new BusinessException(DeliveryManErrorType.HUB_TYPE_MUST_NOT_HAVE_HUB_ID);
            }
            deliveryMan = DeliveryMan.createHubDeliveryMan(
                    userId,
                    realName,
                    slackId,
                    userRole,
                    userStatus,
                    sequence
            );
        } else {
            // COMPANY 타입: hubId 필수
            if (hubId == null) {
                throw new BusinessException(DeliveryManErrorType.HUB_ID_REQUIRED_FOR_COMPANY);
            }
            deliveryMan = DeliveryMan.createCompanyDeliveryMan(
                    userId,
                    hubId,
                    realName,
                    slackId,
                    userRole,
                    userStatus,
                    sequence
            );
        }

        DeliveryMan saved = deliveryManRepository.save(deliveryMan);
        return DeliveryManResponse.Detail.from(saved);
    }

    @Override
    @Transactional
    public void update(
            UUID userId,
            String realName,
            String slackId,
            String userRole,
            String userStatus,
            UUID hubId
    ) {
        if (userId == null) {
            throw new BusinessException(DeliveryManErrorType.USER_ID_REQUIRED);
        }

        DeliveryMan deliveryMan = deliveryManRepository.findByUserIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new BusinessException(DeliveryManErrorType.ALREADY_SOFT_DELETED));

        deliveryMan.updateFromUserEvent(
                realName,
                slackId,
                userRole,
                userStatus,
                hubId
        );
        // JPA dirty checking으로 반영
    }

    @Override
    @Transactional
    public void delete(UUID userId) {
        if (userId == null) {
            throw new BusinessException(DeliveryManErrorType.USER_ID_REQUIRED);
        }

        DeliveryMan deliveryMan = deliveryManRepository.findByUserIdAndDeletedAtIsNull(userId)
                .orElse(null);

        if (deliveryMan == null) {
            // 이미 삭제되었거나, 애초에 생성되지 않았으면 아무 것도 하지 않음 (idempotent)
            return;
        }

        deliveryMan.markDeletedFromUserDeletedEvent();
    }

    // =========================================
    // 2. 관리자용 생성
    // =========================================

    @Override
    @Transactional
    public DeliveryManResponse.Detail createManually(DeliveryManRequest.Create request) {
        if (request == null) {
            throw new BusinessException(DeliveryManErrorType.USER_ID_REQUIRED);
        }

        UUID userId = request.userId();
        UUID hubId = request.hubId();
        DeliveryManType type = request.type();

        if (type == null) {
            throw new BusinessException(DeliveryManErrorType.TYPE_REQUIRED);
        }

        if (deliveryManRepository.existsByUserIdAndDeletedAtIsNull(userId)) {
            throw new BusinessException(DeliveryManErrorType.NO_CHANGES_TO_UPDATE);
        }

        // sequence는 서버에서 계산해서 사용 (dto도 수정했음)
        int sequence = calculateNextSequence(type, hubId);

        // DTO에 들어온 값을 사용하되, 1 미만이면 예외 처리
//        Integer sequence = request.sequence();
//        if (sequence == null || sequence < 1) {
//            throw new BusinessException(DeliveryManErrorType.INVALID_SEQUENCE);
//        }

        DeliveryMan deliveryMan;
        if (type == DeliveryManType.HUB) {
            if (hubId != null) {
                throw new BusinessException(DeliveryManErrorType.HUB_TYPE_MUST_NOT_HAVE_HUB_ID);
            }
            deliveryMan = DeliveryMan.createHubDeliveryMan(
                    userId,
                    request.realName(),
                    request.slackId(),
                    request.userRole(),
                    request.userStatus(),
                    sequence
            );
        } else {
            if (hubId == null) {
                throw new BusinessException(DeliveryManErrorType.HUB_ID_REQUIRED_FOR_COMPANY);
            }
            deliveryMan = DeliveryMan.createCompanyDeliveryMan(
                    userId,
                    hubId,
                    request.realName(),
                    request.slackId(),
                    request.userRole(),
                    request.userStatus(),
                    sequence
            );
        }

        DeliveryMan saved = deliveryManRepository.save(deliveryMan);
        return DeliveryManResponse.Detail.from(saved);
    }

    // =========================================
    // 3. 상태 변경
    // =========================================

    @Override
    @Transactional
    public DeliveryManResponse.Detail changeStatus(UUID deliveryManId, DeliveryManRequest.UpdateStatus newStatus) {
        if (deliveryManId == null) {
            throw new BusinessException(DeliveryManErrorType.USER_ID_REQUIRED);
        }

        DeliveryMan deliveryMan = deliveryManRepository.findByIdAndDeletedAtIsNull(deliveryManId)
                .orElseThrow(() -> new BusinessException(DeliveryManErrorType.ALREADY_SOFT_DELETED));

        deliveryMan.changeDeliveryManStatus(newStatus.status());
        return DeliveryManResponse.Detail.from(deliveryMan);
    }

    // =========================================
    // 4. 배정 / 취소
    // =========================================

    @Override
    @Transactional
    public DeliveryMan assignHubDeliveryMan() {
        // type=HUB, deletedAt IS NULL, sequence ASC 전체 후보
        List<DeliveryMan> candidates =
                deliveryManRepository.findAllByTypeAndDeletedAtIsNullOrderBySequenceAsc(DeliveryManType.HUB);

        DeliveryMan selected = selectBestCandidate(candidates);
        if (selected == null) {
            throw new BusinessException(DeliveryManErrorType.NO_HUB_DELIVERY_MAN_AVAILABLE);
        }

        selected.assignForNewDelivery();
        return selected;
    }

    @Override
    @Transactional
    public DeliveryMan assignCompanyDeliveryMan(UUID hubId) {
        if (hubId == null) {
            throw new BusinessException(DeliveryManErrorType.HUB_ID_REQUIRED_FOR_COMPANY);
        }

        List<DeliveryMan> candidates =
                deliveryManRepository.findAllByHubIdAndTypeAndDeletedAtIsNullOrderBySequenceAsc(
                        hubId, DeliveryManType.COMPANY
                );

        DeliveryMan selected = selectBestCandidate(candidates);
        if (selected == null) {
            throw new BusinessException(DeliveryManErrorType.NO_COMPANY_DELIVERY_MAN_AVAILABLE);
        }

        selected.assignForNewDelivery();
        return selected;
    }

    @Override
    @Transactional
    public void rollbackAssignment(UUID deliveryManId) {
        if (deliveryManId == null) {
            throw new BusinessException(DeliveryManErrorType.USER_ID_REQUIRED);
        }

        DeliveryMan deliveryMan = deliveryManRepository.findByIdAndDeletedAtIsNull(deliveryManId)
                .orElseThrow(() -> new BusinessException(DeliveryManErrorType.ALREADY_SOFT_DELETED));

        deliveryMan.rollbackAssignedDelivery();
    }

    /**
     * 후보 리스트에서 배정 규칙에 맞게 최적의 배송 담당자 선택
     * - 1순위: status == WAITING 인 사람들 중 sequence ASC (이미 정렬되어 있음)
     * - 2순위: WAITING이 없으면 status == DELIVERING 인 사람들 중 deliveryCount 최소
     * - OFFLINE, DELETED는 애초에 후보 리스트에 없어야 함 (레포지토리 조건 + 상태 관리로 보장)
     */
    private DeliveryMan selectBestCandidate(List<DeliveryMan> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        // 1) WAITING 우선
        return candidates.stream()
                .filter(dm -> dm.getStatus() == DeliveryManStatus.WAITING)
                .findFirst()
                .orElseGet(() ->
                        // 2) WAITING 없으면 DELIVERING 중 deliveryCount 최소
                        candidates.stream()
                                .filter(dm -> dm.getStatus() == DeliveryManStatus.DELIVERING)
                                .min(Comparator.comparingInt(DeliveryMan::getDeliveryCount))
                                .orElse(null)
                );
    }

    // =========================================
    // 5. 조회 / 검색
    // =========================================

    @Override
    public DeliveryManResponse.Detail getDetail(UUID deliveryManId) {
        if (deliveryManId == null) {
            throw new BusinessException(DeliveryManErrorType.USER_ID_REQUIRED);
        }

        DeliveryMan deliveryMan = deliveryManRepository.findByIdAndDeletedAtIsNull(deliveryManId)
                .orElseThrow(() -> new BusinessException(DeliveryManErrorType.ALREADY_SOFT_DELETED));

        return DeliveryManResponse.Detail.from(deliveryMan);
    }

    @Override
    public List<DeliveryManResponse.Summary> search(DeliveryManRequest.Search request) {
        List<DeliveryMan> result = deliveryManRepository.search(
                request.hubId(),
                request.type(),
                request.status(),
                request.realName()
        );

        return result.stream()
                .map(DeliveryManResponse.Summary::from)
                .toList();
    }

    // =========================================
    // 내부 유틸 (sequence 계산)
    // =========================================

    private int calculateNextSequence(DeliveryManType type, UUID hubId) {
        Integer maxSequence;
        if (type == DeliveryManType.HUB) {
            maxSequence = deliveryManRepository.findMaxSequenceByTypeAndDeletedAtIsNull(DeliveryManType.HUB);
        } else {
            if (hubId == null) {
                throw new BusinessException(DeliveryManErrorType.HUB_ID_REQUIRED_FOR_COMPANY);
            }
            maxSequence = deliveryManRepository.findMaxSequenceByHubIdAndTypeAndDeletedAtIsNull(
                    hubId, DeliveryManType.COMPANY
            );
        }
        // maxSequence가 null이면 아직 아무도 없는 상태이므로 1부터 시작
        return (maxSequence == null ? 1 : maxSequence + 1);
    }
}
