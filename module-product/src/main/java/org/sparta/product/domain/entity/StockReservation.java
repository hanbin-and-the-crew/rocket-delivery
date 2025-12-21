package org.sparta.product.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.sparta.jpa.entity.BaseEntity;
import org.sparta.product.domain.enums.StockReservationStatus;

import java.util.UUID;

/**
 * 재고 예약 엔티티
 *
 * 핵심:
 * - 외부에서 들어오는 reservationKey(externalReservationKey)는 그대로 저장한다.
 * - Product 내부 멱등/충돌 방지를 위해 internalReservationKey(reservationKey)를 별도로 만들어 저장한다.
 *   internalReservationKey = externalReservationKey + ":" + productId (Stock 기준)
 */
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
                @Index(name = "idx_stock_reservation_status", columnList = "status"),
                @Index(name = "idx_stock_reservation_external_key", columnList = "external_reservation_key"),
                @Index(name = "idx_stock_reservation_external_key_status", columnList = "external_reservation_key,status")
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

    /**
     * 외부 계약 키 (예: orderId.toString())
     */
    @Column(name = "external_reservation_key", nullable = false, length = 128)
    private String externalReservationKey;

    /**
     * Product 내부 멱등/충돌 방지용 키
     * - 기존 컬럼명을 유지(reservation_key)하여 변경 범위를 최소화한다.
     */
    @Column(name = "reservation_key", nullable = false, length = 256)
    private String reservationKey;

    @Column(name = "reserved_quantity", nullable = false)
    private int reservedQuantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private StockReservationStatus status = StockReservationStatus.RESERVED;

    @Version
    private Long version;

    private StockReservation(UUID stockId,
                             String externalReservationKey,
                             String internalReservationKey,
                             int reservedQuantity) {
        if (stockId == null) {
            throw new IllegalArgumentException("stockId must not be null");
        }
        if (externalReservationKey == null || externalReservationKey.isBlank()) {
            throw new IllegalArgumentException("externalReservationKey must not be blank");
        }
        if (internalReservationKey == null || internalReservationKey.isBlank()) {
            throw new IllegalArgumentException("internalReservationKey must not be blank");
        }
        if (reservedQuantity <= 0) {
            throw new IllegalArgumentException("reservedQuantity must be positive");
        }

        this.stockId = stockId;
        this.externalReservationKey = externalReservationKey;
        this.reservationKey = internalReservationKey;
        this.reservedQuantity = reservedQuantity;
        this.status = StockReservationStatus.RESERVED;
    }

    public static StockReservation reserve(UUID stockId,
                                           String externalReservationKey,
                                           String internalReservationKey,
                                           int reservedQuantity) {
        return new StockReservation(stockId, externalReservationKey, internalReservationKey, reservedQuantity);
    }

    public void confirm() {
        if (status == StockReservationStatus.CANCELLED) {
            throw new IllegalStateException("cancelled reservation cannot be confirmed");
        }
        this.status = StockReservationStatus.CONFIRMED;
    }

    public void cancel() {
        if (status == StockReservationStatus.CONFIRMED) {
            throw new IllegalStateException("confirmed reservation cannot be cancelled");
        }
        this.status = StockReservationStatus.CANCELLED;
    }

    // StockReservation 엔티티에 추가
    public void compensateCancel() {
        this.status = StockReservationStatus.CANCELLED;
    }


    public boolean isConfirmed() {
        return status == StockReservationStatus.CONFIRMED;
    }

    public boolean isCancelled() {
        return status == StockReservationStatus.CANCELLED;
    }
}
