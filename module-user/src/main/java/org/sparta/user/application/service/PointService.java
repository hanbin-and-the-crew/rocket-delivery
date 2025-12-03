package org.sparta.user.application.service;

import lombok.RequiredArgsConstructor;
import org.sparta.common.error.BusinessException;
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

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PointService {
    private final PointRepository pointRepository;
    private final PointReservationRepository reservationRepository;

    /**
     * 포인트 예약 (결제 시작 단계)
     */
    public PointResponse.PointReservationResult reservePoints(Long userId, Integer requiredAmount, String paymentId) {
        // FIFO: AVAILABLE이고 만료되지 않은 포인트를 유효 기간이 오래된 순서로 조회
        List<Point> availablePoints = pointRepository.findUsablePoints(
                userId,
                PointStatus.AVAILABLE,
                LocalDateTime.now(),
                Sort.by(Sort.Direction.ASC, "expiryDate")
        );

        int remainingAmount = requiredAmount;
        List<PointReservation> reservations = new ArrayList<>();

        for (Point point : availablePoints) {
            if (remainingAmount <= 0) break;

            int reserveAmount = Math.min(point.getAmount(), remainingAmount);

            // Point 상태 변경
            point.setStatus(PointStatus.RESERVED);
            point.setReservedAt(LocalDateTime.now());
            pointRepository.save(point);

            // Reservation 기록
            PointReservation reservation = new PointReservation();
            reservation.setPointId(point.getId());
            reservation.setPaymentId(paymentId);
            reservation.setReservedAmount(reserveAmount);
            reservation.setStatus(ReservationStatus.RESERVED);
            reservationRepository.save(reservation);

            reservations.add(reservation);
            remainingAmount -= reserveAmount;
        }

        // 부족한 경우
        if (remainingAmount > 0) {
            // 예약한 것들 롤백
            rollbackReservations(paymentId);
            throw new BusinessException(PointErrorType.POINT_IS_INSUFFICIENT);
        }

        return new PointResponse.PointReservationResult(requiredAmount, reservations);
    }

    /**
     * 포인트 차감 확정 (결제 완료 단계)
     */
    public void confirmPointUsage(String paymentId) {
        List<PointReservation> reservations = reservationRepository.findByPaymentIdAndStatus(
                paymentId,
                ReservationStatus.RESERVED
        );

        for (PointReservation reservation : reservations) {
            Point point = pointRepository.findById(reservation.getPointId())
                    .orElseThrow(() -> new BusinessException(PointErrorType.POINT_NOT_FOUND));

            point.setStatus(PointStatus.USED);
            point.setUsedAt(LocalDateTime.now());
            pointRepository.save(point);

            reservation.setStatus(ReservationStatus.CONFIRMED);
            reservationRepository.save(reservation);
        }
    }

    /**
     * 포인트 예약 취소 (결제 실패 시 보상 트랜잭션)
     */
    public void rollbackReservations(String paymentId) {
        List<PointReservation> reservations = reservationRepository.findByPaymentIdAndStatus(
                paymentId,
                ReservationStatus.RESERVED
        );

        for (PointReservation reservation : reservations) {
            Point point = pointRepository.findById(reservation.getPointId())
                    .orElseThrow(() -> new BusinessException(PointErrorType.POINT_NOT_FOUND));

            point.setStatus(PointStatus.AVAILABLE);
            point.setReservedAt(null);
            pointRepository.save(point);

            reservation.setStatus(ReservationStatus.CANCELLED);
            reservationRepository.save(reservation);
        }
    }

    /**
     * 포인트 적립 (구매 후). 이거까진 이용안할듯
     */
    public void addPoints(Long userId, Integer amount, LocalDateTime expiryDate) {
        Point point = new Point();
        point.setUserId(userId);
        point.setAmount(amount);
        point.setExpiryDate(expiryDate);
        pointRepository.save(point);
    }
}
