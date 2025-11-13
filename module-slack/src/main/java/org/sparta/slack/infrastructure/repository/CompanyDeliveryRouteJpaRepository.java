package org.sparta.slack.infrastructure.repository;

import org.sparta.slack.domain.entity.CompanyDeliveryRoute;
import org.sparta.slack.domain.enums.RouteStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompanyDeliveryRouteJpaRepository extends JpaRepository<CompanyDeliveryRoute, UUID> {

    Optional<CompanyDeliveryRoute> findByDeliveryId(UUID deliveryId);

    List<CompanyDeliveryRoute> findAllByScheduledDate(LocalDate date);

    List<CompanyDeliveryRoute> findAllByScheduledDateAndStatusIn(LocalDate date, Collection<RouteStatus> statuses);
}
