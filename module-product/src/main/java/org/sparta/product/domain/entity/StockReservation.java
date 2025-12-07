package org.sparta.product.domain.entity;


import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.sparta.jpa.entity.BaseEntity;
import org.sparta.product.domain.enums.StockReservationStatus;

import java.util.UUID;

@Entity
@Table(
        name = "stock_reservations",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_stock_reservation_reservation_key",
                        columnNames = {"reservation_key"}
                )
        },
        indexes = {
                @Index(name = "idx_stock_reservation_stock_id", columnList = "stock_id"),
                @Index(name = "idx_stock_reservation_status", columnList = "status")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockReservation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "stock_id", nullable = false, columnDefinition = "uuid")
    private UUID stockId;

    @Column(name = "reservation_key", nullable = false, length = 100)
    private String reservationKey;

    @Column(name = "reserved_quantity", nullable = false)
    private int reservedQuantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private StockReservationStatus status;

    @Version
    @Column(name = "version")
    private Long version;

    // == 생성 메서드 == //

    private StockReservation(UUID stockId,
                             String reservationKey,
                             int reservedQuantity) {
        if (stockId == null) {
            throw new IllegalArgumentException("stockId must not be null");
        }
        if (reservationKey == null || reservationKey.isBlank()) {
            throw new IllegalArgumentException("reservationKey must not be blank");
        }
        if (reservedQuantity <= 0) {
            throw new IllegalArgumentException("reservedQuantity must be positive");
        }

        this.stockId = stockId;
        this.reservationKey = reservationKey;
        this.reservedQuantity = reservedQuantity;
        this.status = StockReservationStatus.RESERVED;
    }

    /**
     * 예약 생성 팩토리 메서드
     */
    public static StockReservation create(UUID stockId,
                                          String reservationKey,
                                          int reservedQuantity) {
        return new StockReservation(stockId, reservationKey, reservedQuantity);
    }

    // == 비즈니스 메서드 == //

    /**
     * 예약 확정 (결제 성공 시 호출)
     *
     * 멱등성을 고려하여 이미 CONFIRMED 상태라면 그대로 무시한다.
     * CANCELLED 상태에서 CONFIRM을 시도하는 것은 잘못된 흐름이므로 예외를 던진다.
     */
    public void confirm() {
        if (this.status == StockReservationStatus.CONFIRMED) {
            // 멱등 처리: 이미 확정된 예약이면 그냥 무시
            return;
        }
        if (this.status == StockReservationStatus.CANCELLED) {
            throw new IllegalStateException("이미 취소된 예약은 확정할 수 없습니다.");
        }
        this.status = StockReservationStatus.CONFIRMED;
    }

    /**
     * 예약 취소 (주문 취소 / 결제 실패 시 호출)
     *
     * 멱등성을 고려하여 이미 CANCELLED 상태라면 그대로 무시한다.
     * CONFIRMED 상태에서 CANCEL을 시도하는 것은 잘못된 흐름이므로 예외를 던진다.
     */
    public void cancel() {
        if (this.status == StockReservationStatus.CANCELLED) {
            // 멱등 처리: 이미 취소된 예약이면 그냥 무시
            return;
        }
        if (this.status == StockReservationStatus.CONFIRMED) {
            throw new IllegalStateException("이미 확정된 예약은 취소할 수 없습니다.");
        }
        this.status = StockReservationStatus.CANCELLED;
    }

    public boolean isReserved() {
        return this.status == StockReservationStatus.RESERVED;
    }

    public boolean isConfirmed() {
        return this.status == StockReservationStatus.CONFIRMED;
    }

    public boolean isCancelled() {
        return this.status == StockReservationStatus.CANCELLED;
    }

}
