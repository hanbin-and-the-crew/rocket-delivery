package org.sparta.slack.application.service.route;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.slack.application.port.out.SlackRecipientFinder;
import org.sparta.slack.domain.entity.CompanyDeliveryRoute;
import org.sparta.slack.domain.entity.UserSlackView;
import org.sparta.slack.domain.enums.RouteStatus;
import org.sparta.slack.domain.enums.UserRole;
import org.sparta.slack.domain.repository.CompanyDeliveryRouteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryAssignmentService {

    private final CompanyDeliveryRouteRepository routeRepository;
    private final SlackRecipientFinder recipientFinder;

    private static final Set<UserRole> ROUTE_ROLES = Set.of(
            UserRole.DELIVERY_MANAGER,
            UserRole.COMPANY_MANAGER
    );

    @Transactional
    public List<CompanyDeliveryRoute> assign(LocalDate date) {
        List<CompanyDeliveryRoute> allRoutes = routeRepository.findAllByScheduledDate(date);
        List<CompanyDeliveryRoute> targets = allRoutes.stream()
                .filter(route -> route.getStatus() == RouteStatus.PENDING || route.getStatus() == RouteStatus.FAILED)
                .collect(Collectors.toList());

        Map<UUID, Integer> loadCounter = new HashMap<>();
        allRoutes.stream()
                .filter(route -> route.getDeliveryManagerId() != null)
                .forEach(route -> loadCounter.merge(route.getDeliveryManagerId(), 1, Integer::sum));

        Map<UUID, Integer> managerSequence = new HashMap<>();
        List<CompanyDeliveryRoute> assigned = new ArrayList<>();

        Map<UUID, List<CompanyDeliveryRoute>> byHub = targets.stream()
                .collect(Collectors.groupingBy(CompanyDeliveryRoute::getOriginHubId));

        for (Map.Entry<UUID, List<CompanyDeliveryRoute>> entry : byHub.entrySet()) {
            UUID hubId = entry.getKey();
            List<UserSlackView> managers = recipientFinder.findApprovedByHubAndRoles(hubId, ROUTE_ROLES)
                    .stream()
                    .filter(view -> view.getSlackId() != null && !view.getSlackId().isBlank())
                    .collect(Collectors.toList());

            if (managers.isEmpty()) {
                log.warn("허브 {}에 배정 가능한 배송 담당자가 없습니다", hubId);
                continue;
            }

            for (CompanyDeliveryRoute route : entry.getValue()) {
                UserSlackView manager = selectManager(managers, loadCounter);
                if (manager == null) {
                    continue;
                }
                int order = managerSequence.merge(manager.getUserId(), 1, Integer::sum);
                route.assignManager(
                        manager.getUserId(),
                        manager.getRealName(),
                        manager.getSlackId(),
                        order
                );
                loadCounter.merge(manager.getUserId(), 1, Integer::sum);
                assigned.add(routeRepository.save(route));
            }
        }

        return assigned;
    }

    private UserSlackView selectManager(List<UserSlackView> managers, Map<UUID, Integer> loadCounter) {
        return managers.stream()
                .min(Comparator
                        .comparing((UserSlackView view) -> loadCounter.getOrDefault(view.getUserId(), 0))
                        .thenComparing(UserSlackView::getRealName))
                .orElse(null);
    }
}
