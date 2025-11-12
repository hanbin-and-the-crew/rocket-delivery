package org.sparta.order.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.sparta.jpa.entity.BaseEntity;

import java.util.UUID;

@Entity
@Getter
@Table(name = "p_payment")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String paymentName;

    public Payment(String paymentName) {
        this.paymentName = paymentName;
    }

    public static Payment create(String paymentName) {
        return new Payment(paymentName);
    }
}