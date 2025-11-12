package org.sparta.order.infrastructure.repository;

import org.sparta.order.domain.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PaymentJpaRepository extends JpaRepository<Payment, UUID> {

}