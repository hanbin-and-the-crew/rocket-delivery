package org.sparta.user.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sparta.user.domain.entity.Point;
import org.sparta.user.domain.entity.PointReservation;
import org.sparta.user.domain.enums.PointStatus;
import org.sparta.user.domain.enums.ReservationStatus;
import org.sparta.user.domain.repository.PointRepository;
import org.sparta.user.domain.repository.PointReservationRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class PointServiceUnitTest {

    @Mock
    private PointRepository pointRepository;

    @Mock
    private PointReservationRepository reservationRepository;

    @InjectMocks
    private PointService pointService;

    @Test
    @DisplayName("만료된 예약은 rollback 처리된다")
    void expireReservations_ShouldRollbackExpiredReservations() {

        // given
        UUID pointId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        PointReservation expiredReservation = PointReservation.create(pointId, orderId, 500L, ReservationStatus.RESERVED);
        expiredReservation.updateReservedAt(LocalDateTime.now().minusSeconds(31)); // TTL 30초

        Point point = Point.create(UUID.randomUUID(), 1000L, 0L, 500L, LocalDateTime.now().plusDays(1), PointStatus.AVAILABLE);

        given(reservationRepository.findByStatusAndReservedAtBefore(eq(ReservationStatus.RESERVED), any(LocalDateTime.class)))
                .willReturn(List.of(expiredReservation));

        // rollbackReservations 내부 조회 Mock
        given(reservationRepository.findByOrderIdAndStatus(orderId, ReservationStatus.RESERVED))
                .willReturn(List.of(expiredReservation));

        given(pointRepository.findById(pointId)).willReturn(Optional.of(point));

        // when
        pointService.expireReservations();

        // then
        assertThat(expiredReservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(point.getReservedAmount()).isEqualTo(0L);

        then(reservationRepository).should().save(expiredReservation);
        then(pointRepository).should().save(point);
    }
}
