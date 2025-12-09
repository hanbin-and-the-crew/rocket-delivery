//package org.sparta.order.domain.entity;
//
//import jakarta.persistence.*;
//import lombok.AccessLevel;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//import org.sparta.jpa.entity.BaseEntity;
//import org.sparta.order.domain.enumeration.PaymentStatus;
//
//import java.math.BigDecimal;
//import java.time.LocalDateTime;
//import java.util.UUID;
//
//@Entity
//@Getter
//@Table(name = "p_payment")
//@NoArgsConstructor(access = AccessLevel.PROTECTED)
//public class Payment extends BaseEntity {
//    @Id
//    @GeneratedValue(strategy = GenerationType.UUID)
//    private UUID id;
//
//    private UUID orderId;
//
//    private UUID productId;
//
//    private Integer quantity;
//
//    @Enumerated(EnumType.STRING)
//    private PaymentStatus status; // PENDING, COMPLETED, FAILED
//
//    private BigDecimal amount;
//
//    public static Payment create(UUID orderId, UUID productId, Integer quantity, BigDecimal amount) {
//        Payment payment = new Payment();
//        payment.orderId = orderId;
//        payment.productId = productId;
//        payment.quantity = quantity;
//        payment.amount = amount;
//        payment.status = PaymentStatus.PENDING;
//        payment.createdAt = LocalDateTime.now();
//        return payment;
//    }
//    public boolean isValid() {
//        return this.quantity > 0;
//    }
//    public void complete() {
//        this.status = PaymentStatus.COMPLETED;
//    }
//
//    public void fail() {
//        this.status = PaymentStatus.FAILED;
//    }
//}