package org.sparta.payment.domain.repository;

import org.sparta.payment.domain.entity.Refund;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefundRepository {

    Refund save(Refund refund);

    Optional<Refund> findById(UUID id);

    List<Refund> findByPaymentId(UUID paymentId);
}
