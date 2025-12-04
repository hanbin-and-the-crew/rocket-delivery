package org.sparta.user.application.service;

import lombok.RequiredArgsConstructor;
import org.sparta.common.error.BusinessException;
import org.sparta.user.application.command.PointCommand;
import org.sparta.user.domain.entity.Point;
import org.sparta.user.domain.entity.PointReservation;
import org.sparta.user.domain.enums.PointStatus;
import org.sparta.user.domain.enums.ReservationStatus;
import org.sparta.user.domain.error.PointErrorType;
import org.sparta.user.domain.repository.PointRepository;
import org.sparta.user.domain.repository.PointReservationRepository;
import org.sparta.user.presentation.dto.response.PointResponse;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PointService {
    private final PointRepository pointRepository;
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
        boolean exists = reservationRepository.existsByOrderId(orderId);
        if (exists) {
            throw new BusinessException(PointErrorType.DUPLICATE_ORDER_ID);
        }

        // FIFO: AVAILABLE이고 만료되지 않은 포인트를 유효 기간이 오래된 순서로 조회
        List<Point> availablePoints = pointRepository.findUsablePoints(
                userId,
                PointStatus.AVAILABLE,
                LocalDateTime.now(),
                Sort.by(Sort.Direction.ASC, "expiryDate")
        );

        Long remainingAmount = requiredAmount;
        List<PointReservation> reservations = new ArrayList<>();

        for (Point point : availablePoints) {
            if (remainingAmount <= 0L) break;

            Long availableInPoint = point.getAvailableAmount(); // 1000 - 0 - 0 = 1000
            if (availableInPoint <= 0L) continue; // 사용 가능한 게 없으면 스킵

            Long reserveAmount = Math.min(availableInPoint, remainingAmount);

            // Point 상태 변경
            point.setReservedAmount(point.getReservedAmount() + reserveAmount);
            pointRepository.save(point);

            // Reservation 기록
            PointReservation reservation = new PointReservation();
            reservation.setPointId(point.getId());
            reservation.setOrderId(orderId);
            reservation.setReservedAmount(reserveAmount);
            reservation.setStatus(ReservationStatus.RESERVED);
            reservationRepository.save(reservation);

            reservations.add(reservation);
            remainingAmount -= reserveAmount;
        }

        // 부족한 경우
        if (remainingAmount > 0) {
            // 예약한 것들 롤백
            rollbackReservations(orderId);
            throw new BusinessException(PointErrorType.POINT_IS_INSUFFICIENT);
        }

        return new PointResponse.PointReservationResult(requiredAmount, reservations);
    }

    /**
     * 포인트 차감 확정 (결제 완료 단계)
     */
    @Transactional
    public void confirmPointUsage(PointCommand.ConfirmPoint command) {
        UUID orderId = command.orderId();

        List<PointReservation> reservations = reservationRepository.findByOrderIdAndStatus(
                orderId,
                ReservationStatus.RESERVED
        );

        for (PointReservation reservation : reservations) {
            Point point = pointRepository.findById(reservation.getPointId())
                    .orElseThrow(() -> new BusinessException(PointErrorType.POINT_NOT_FOUND));

            point.setReservedAmount(point.getReservedAmount() - reservation.getReservedAmount());
            point.setUsedAmount(point.getUsedAmount() + reservation.getReservedAmount());
            pointRepository.save(point);

            reservation.setStatus(ReservationStatus.CONFIRMED);
            reservationRepository.save(reservation);
        }
    }

    /**
     * 포인트 예약 취소 (결제 실패 시 보상 트랜잭션)
     */
    public void rollbackReservations(UUID orderId) {
        List<PointReservation> reservations = reservationRepository.findByOrderIdAndStatus(
                orderId,
                ReservationStatus.RESERVED
        );

        for (PointReservation reservation : reservations) {
            Point point = pointRepository.findById(reservation.getPointId())
                    .orElseThrow(() -> new BusinessException(PointErrorType.POINT_NOT_FOUND));

            // reservedAmount만 감소 (복구)
            point.setReservedAmount(point.getReservedAmount() - reservation.getReservedAmount());
            pointRepository.save(point);

            reservation.setStatus(ReservationStatus.CANCELLED);
            reservationRepository.save(reservation);
        }
    }

    /**
     * 포인트 적립 (구매 후). 이거까진 이용안할듯
     */
    @Transactional
    public void addPoints(UUID userId, Long amount, LocalDateTime expiryDate) {
        Point point = new Point();
        point.setUserId(userId);
        point.setAmount(amount);
        point.setExpiryDate(expiryDate);
        pointRepository.save(point);
    }

    /**
     * 현재 User 포인트 계산
     */
    @Transactional
    public PointResponse.PointSummary getPoint(UUID userId) {
        List<Point> activePoints = pointRepository.findUsablePoints(
                userId,
                PointStatus.AVAILABLE,
                LocalDateTime.now(),
                Sort.by(Sort.Direction.ASC, "createdAt")

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
}
