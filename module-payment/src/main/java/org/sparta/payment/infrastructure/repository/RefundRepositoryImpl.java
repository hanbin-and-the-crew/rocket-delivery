package org.sparta.payment.infrastructure.repository;

import lombok.RequiredArgsConstructor;
import org.sparta.payment.domain.entity.Refund;

import org.sparta.payment.domain.repository.RefundRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class RefundRepositoryImpl implements RefundRepository {

    private final RefundJpaRepository jpa;


    public Refund save(Refund refund) {
        return jpa.save(refund);
    }


    public Optional<Refund> findById(UUID id) {
        return jpa.findById(id);
    }


    public List<Refund> findByPaymentId(UUID paymentId) {
        return jpa.findByPaymentId(paymentId);
    }
}
