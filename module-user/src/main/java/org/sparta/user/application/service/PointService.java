package org.sparta.user.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.common.error.BusinessException;
import org.sparta.user.application.command.PointCommand;
import org.sparta.user.application.dto.PointServiceResult;
import org.sparta.user.domain.entity.Point;
import org.sparta.user.domain.entity.PointReservation;
import org.sparta.user.domain.enums.PointStatus;
import org.sparta.user.domain.enums.ReservationStatus;
import org.sparta.user.domain.error.PointErrorType;
import org.sparta.user.domain.error.UserErrorType;
import org.sparta.user.domain.repository.PointRepository;
import org.sparta.user.domain.repository.PointReservationRepository;
import org.sparta.user.domain.repository.UserRepository;
import org.sparta.user.presentation.dto.response.PointResponse;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.springframework.data.domain.Sort.Direction.ASC;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PointService {
    private final PointRepository pointRepository;
    private final UserRepository userRepository;
    private final PointReservationRepository reservationRepository;

    /**
     * 포인트 예약 (결제 시작 단계)
     */
    @Transactional
    public PointResponse.PointReservationResult reservePoints(PointCommand.ReservePoint command) {

        UUID userId = command.userId();
        Long requiredAmount = command.requestPoint();
        UUID orderId = command.orderId();

        // orderId 기반 중복 체크
        if (reservationRepository.existsByOrderId(orderId)) {
            throw new BusinessException(PointErrorType.DUPLICATE_ORDER_ID);
        }

        // User 존재하는지 확인
        userRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(UserErrorType.USER_NOT_FOUND));

        // FIFO: AVAILABLE이고 만료되지 않은 포인트를 유효 기간이 오래된 순서로 조회
        List<Point> availablePoints = pointRepository.findUsablePoints(
                userId,
                PointStatus.AVAILABLE,
                LocalDateTime.now(),
                Sort.by(ASC, "expiryDate")
        );

        // 조회하자마자 해당 user의 총 가능한 포인트 합
        long totalAvailable = availablePoints.stream()
                .mapToLong(Point::getAvailableAmount)
                .sum();

        // 포인트가 부족한 경우
        if (totalAvailable < requiredAmount) {
            log.info("사용 가능한 총 포인트: {}, 요청 포인트: {}", totalAvailable, requiredAmount);
            throw new BusinessException(PointErrorType.POINT_IS_INSUFFICIENT);
        }

        Long remainingAmount = requiredAmount;
        List<PointReservation> reservations = new ArrayList<>();

        for (Point point : availablePoints) {
            if (remainingAmount <= 0L) break;

            Long availableInPoint = point.getAvailableAmount(); // 1000 - 0 - 0 = 1000
            if (availableInPoint <= 0L) continue; // 사용 가능한 게 없으면 스킵

            Long reserveAmount = Math.min(availableInPoint, remainingAmount);

            // Point 상태 변경
            point.updateReservedAmount(point.getReservedAmount() + reserveAmount);
            pointRepository.save(point);

            // Reservation 기록
            PointReservation reservation = PointReservation.create(
                    point.getId(),
                    orderId,
                    reserveAmount,
                    ReservationStatus.RESERVED
            );
            reservationRepository.save(reservation);

            reservations.add(reservation);
            remainingAmount -= reserveAmount;
        }

        return PointResponse.PointReservationResult.of(requiredAmount, reservations);
    }

    /**
     * 포인트 차감 확정 (결제 완료 단계)
     */
    @Transactional
    public PointServiceResult.Confirm confirmPointUsage(PointCommand.ConfirmPoint command) {
        UUID orderId = command.orderId();

        List<PointReservation> reservations = reservationRepository.findByOrderIdAndStatus(
                orderId,
                ReservationStatus.RESERVED
        );

        Long discountAmount = 0L;
        List<PointServiceResult.PointUsageDetail> confirmedDetails = new ArrayList<>();

        for (PointReservation reservation : reservations) {
            Point point = pointRepository.findById(reservation.getPointId())
                    .orElseThrow(() -> new BusinessException(PointErrorType.POINT_NOT_FOUND));

            point.updateReservedAmount(point.getReservedAmount() - reservation.getReservedAmount());
            point.updateUsedAmount(point.getUsedAmount() + reservation.getReservedAmount());
            discountAmount += reservation.getReservedAmount();
            pointRepository.save(point);

            reservation.updateStatus(ReservationStatus.CONFIRMED);
            reservationRepository.save(reservation);

            confirmedDetails.add(new PointServiceResult.PointUsageDetail(
                    reservation.getPointId(),
                    reservation.getReservedAmount()
            ));
        }

        return new PointServiceResult.Confirm(orderId, discountAmount, confirmedDetails);
    }

    /**
     * 예약 취소
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void rollbackReservations(UUID orderId) {
        log.info("예약 취소 시작");
        List<PointReservation> reservations = reservationRepository.findByOrderIdAndStatus(
                orderId,
                ReservationStatus.RESERVED
        );

        for (PointReservation reservation : reservations) {
            Point point = pointRepository.findById(reservation.getPointId())
                    .orElseThrow(() -> new BusinessException(PointErrorType.POINT_NOT_FOUND));

            // reservedAmount만 감소 (복구)
            point.updateReservedAmount(point.getReservedAmount() - reservation.getReservedAmount());
            pointRepository.save(point);

            reservation.updateStatus(ReservationStatus.CANCELLED);
            reservationRepository.save(reservation);
        }
    }

    /**
     * 현재 User 포인트 계산
     */
    public PointResponse.PointSummary getPoint(UUID userId) {
        List<Point> activePoints = pointRepository.findUsablePoints(
                userId,
                PointStatus.AVAILABLE,
                LocalDateTime.now()
        );

        // 포인트 통계 계산
        Long totalAmount = activePoints.stream()
                .mapToLong(Point::getAmount)
                .sum();

        Long totalReservedAmount = activePoints.stream()
                .mapToLong(Point::getReservedAmount)
                .sum();

        Long totalUsedAmount = activePoints.stream()
                .mapToLong(Point::getUsedAmount)
                .sum();

        Long availableAmount = activePoints.stream()
                .mapToLong(Point::getAvailableAmount)
                .sum();

        return PointResponse.PointSummary.of(totalAmount, totalReservedAmount, totalUsedAmount, availableAmount);
    }

    /**
     * 포인트 자동 만료
     */
    @Transactional
    public void expireReservations() {
        LocalDateTime now = LocalDateTime.now();
        int ttlMinutes = 5; // 예약 만료 5분
        LocalDateTime expireThreshold = now.minusMinutes(ttlMinutes);

        // 만료된 예약 조회
        List<PointReservation> expiredReservations = reservationRepository.findByStatusAndReservedAtBefore(
                ReservationStatus.RESERVED, expireThreshold
        );

        for (PointReservation reservation : expiredReservations) {
            rollbackReservations(reservation.getOrderId());
            log.info("예약 만료 처리: orderId={}, pointReservationId={}", reservation.getOrderId(), reservation.getId());
        }
    }

    /**
     * 포인트 적립 (구매 후). 이거까진 이용안할듯
     */
    @Transactional
    public void addPoints(UUID userId, Long amount, LocalDateTime expiryDate) {
        Point point = Point.create( // 이용 안할거라 일단 토대만 작성.
                userId,
                amount,
                0L,
                0L,
                expiryDate,
                PointStatus.AVAILABLE
        );
        pointRepository.save(point);
    }
}
