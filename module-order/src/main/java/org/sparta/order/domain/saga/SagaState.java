package org.sparta.order.domain.saga;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "p_order_saga_states")
@Data @Builder @AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SagaState {
    @Id @GeneratedValue
    private UUID id;
    private UUID orderId;
    private String orderStatus;
    private String paymentStatus = "PENDING";
    private String deliveryStatus = "PENDING";
    private String overallStatus = "IN_PROGRESS";
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
}
