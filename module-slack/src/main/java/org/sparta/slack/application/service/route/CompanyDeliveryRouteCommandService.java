package org.sparta.slack.application.service.route;

import lombok.RequiredArgsConstructor;
import org.sparta.slack.application.command.RouteRegisterCommand;
import org.sparta.slack.domain.entity.CompanyDeliveryRoute;
import org.sparta.slack.domain.repository.CompanyDeliveryRouteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompanyDeliveryRouteCommandService {

    private final CompanyDeliveryRouteRepository routeRepository;

    @Transactional
    public CompanyDeliveryRoute register(RouteRegisterCommand command) {
        return routeRepository.findByDeliveryId(command.deliveryId())
                .map(existing -> {
                    existing.updateBasicInfo(
                            command.scheduledDate(),
                            command.originHubName(),
                            command.originAddress(),
                            command.destinationCompanyName(),
                            command.destinationAddress(),
                            command.stops()
                    );
                    return routeRepository.save(existing);
                })
                .orElseGet(() -> routeRepository.save(
                        CompanyDeliveryRoute.create(
                                command.deliveryId(),
                                command.scheduledDate(),
                                command.originHubId(),
                                command.originHubName(),
                                command.originAddress(),
                                command.destinationCompanyId(),
                                command.destinationCompanyName(),
                                command.destinationAddress(),
                                command.stops()
                        )
                ));
    }
}
