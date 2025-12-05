package org.sparta.order.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "idempotency_records")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IdempotencyRecord {

    @Id
    private String idempotencyKey;

    @Column(nullable = false)
    private String orderId;

    @Column(columnDefinition = "TEXT")
    private String responseBody;

    @Column(nullable = false)
    private int statusCode;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    public static IdempotencyRecord create(String idempotencyKey, String orderId,
                                           String responseBody, int statusCode) {
        IdempotencyRecord record = new IdempotencyRecord();
        record.idempotencyKey = idempotencyKey;
        record.orderId = orderId;
        record.responseBody = responseBody;
        record.statusCode = statusCode;
        record.createdAt = LocalDateTime.now();
        record.expiresAt = LocalDateTime.now().plusHours(2); // 2시간 후 만료
        return record;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
