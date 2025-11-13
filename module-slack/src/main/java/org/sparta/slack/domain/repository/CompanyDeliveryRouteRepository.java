package org.sparta.slack.domain.repository;

import org.sparta.slack.domain.entity.CompanyDeliveryRoute;
import org.sparta.slack.domain.enums.RouteStatus;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 업체 배송 경로 저장소 추상화
 */
public interface CompanyDeliveryRouteRepository {

    CompanyDeliveryRoute save(CompanyDeliveryRoute route);

    Optional<CompanyDeliveryRoute> findById(UUID id);

    Optional<CompanyDeliveryRoute> findByDeliveryId(UUID deliveryId);

    List<CompanyDeliveryRoute> findAllByScheduledDate(LocalDate date);

    List<CompanyDeliveryRoute> findAllByScheduledDateAndStatusIn(LocalDate date, Collection<RouteStatus> statuses);
}
