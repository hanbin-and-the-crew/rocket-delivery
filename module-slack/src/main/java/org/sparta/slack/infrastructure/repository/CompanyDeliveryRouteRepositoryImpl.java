package org.sparta.slack.infrastructure.repository;

import lombok.RequiredArgsConstructor;
import org.sparta.slack.domain.entity.CompanyDeliveryRoute;
import org.sparta.slack.domain.enums.RouteStatus;
import org.sparta.slack.domain.repository.CompanyDeliveryRouteRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class CompanyDeliveryRouteRepositoryImpl implements CompanyDeliveryRouteRepository {

    private final CompanyDeliveryRouteJpaRepository routeJpaRepository;

    @Override
    public CompanyDeliveryRoute save(CompanyDeliveryRoute route) {
        return routeJpaRepository.save(route);
    }

    @Override
    public Optional<CompanyDeliveryRoute> findById(UUID id) {
        return routeJpaRepository.findById(id);
    }

    @Override
    public Optional<CompanyDeliveryRoute> findByDeliveryId(UUID deliveryId) {
        return routeJpaRepository.findByDeliveryId(deliveryId);
    }

    @Override
    public List<CompanyDeliveryRoute> findAllByScheduledDate(LocalDate date) {
        return routeJpaRepository.findAllByScheduledDate(date);
    }

    @Override
    public List<CompanyDeliveryRoute> findAllByScheduledDateAndStatusIn(LocalDate date, Collection<RouteStatus> statuses) {
        return routeJpaRepository.findAllByScheduledDateAndStatusIn(date, statuses);
    }
}
