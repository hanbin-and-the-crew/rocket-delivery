package org.sparta.slack.application.service;

import lombok.RequiredArgsConstructor;
import org.sparta.slack.domain.entity.CompanyDeliveryRoute;
import org.sparta.slack.domain.repository.CompanyDeliveryRouteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CompanyDeliveryRouteQueryService {

    private final CompanyDeliveryRouteRepository routeRepository;

    @Transactional(readOnly = true)
    public List<CompanyDeliveryRoute> findByDate(LocalDate date) {
        return routeRepository.findAllByScheduledDate(date);
    }
}
