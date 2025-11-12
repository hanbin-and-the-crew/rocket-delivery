package org.sparta.hub.application;

import lombok.RequiredArgsConstructor;
import org.sparta.hub.application.route.RouteLeg;
import org.sparta.hub.domain.entity.Hub;
import org.sparta.hub.domain.model.HubStatus;
import org.sparta.hub.domain.repository.HubRepository;
import org.sparta.hub.presentation.dto.response.RoutePlanResponse;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Cacheable(value = "hubRoutes", key = "#from.name + '->' + #to.name")
@Transactional(readOnly = true)
public class HubRoutePlanner {

    private static final double MAX_LEG_KM = 200.0;
    private static final int MAX_HOPS = 20;
    private static final double KM_TO_MIN = 1.0; // 1km = 1분으로 단순화

    private final HubRepository hubRepository;

    @Cacheable(cacheNames = "routePlan", key = "'routePlan::' + #sourceId + '::' + #targetId")
    public RoutePlanResponse plan(UUID sourceId, UUID targetId) {
        if (sourceId.equals(targetId)) {
            throw new IllegalArgumentException("출발 허브와 도착 허브가 같습니다.");
        }

        Map<UUID, Hub> activeMap = hubRepository.findAllByStatus(HubStatus.ACTIVE)
                .stream().collect(Collectors.toMap(Hub::getHubId, h -> h));

        Hub source = optionalOrThrow(activeMap.get(sourceId), "출발 허브를 찾을 수 없습니다.");
        Hub target = optionalOrThrow(activeMap.get(targetId), "도착 허브를 찾을 수 없습니다.");

        double directKm = distanceKm(source, target);
        if (directKm <= MAX_LEG_KM) {
            RouteLeg leg = new RouteLeg(source.getHubId(), target.getHubId(), round2(directKm), (int)Math.round(directKm * KM_TO_MIN));
            return new RoutePlanResponse(sourceId, targetId, round2(directKm), leg.estimatedMinutes(), List.of(leg));
        }

        // Relay: 그리디 릴레이
        Set<UUID> visited = new HashSet<>();
        visited.add(source.getHubId());
        List<RouteLeg> legs = new ArrayList<>();

        Hub current = source;
        int hops = 0;
        while (distanceKm(current, target) > MAX_LEG_KM) {
            if (hops++ > MAX_HOPS) {
                throw new IllegalStateException("경유 허브 탐색 중 홉 제한을 초과했습니다.");
            }

            // 후보: 200km 이내, 아직 방문 X, 자신/목적지 제외
            final Hub currentHub = current;  // ← while 내부에서 매번 새 final 변수로 복사

            List<Hub> candidates = activeMap.values().stream()
                    .filter(h -> !visited.contains(h.getHubId()))
                    .filter(h -> !h.getHubId().equals(currentHub.getHubId()))
                    .filter(h -> !h.getHubId().equals(target.getHubId()))
                    .filter(h -> distanceKm(currentHub, h) <= MAX_LEG_KM)
                    .toList();

            if (candidates.isEmpty()) {
                throw new IllegalStateException("200km 내 경유 후보 허브가 없습니다.");
            }

            // 1순위: 목적지까지 더 가까워지는 후보
            double currentToTarget = distanceKm(current, target);
            List<Hub> forward = candidates.stream()
                    .filter(h -> distanceKm(h, target) < currentToTarget)
                    .toList();

            Hub next;
            if (!forward.isEmpty()) {
                // 목적지와의 남은 거리 최소
                next = forward.stream()
                        .min(Comparator.comparingDouble(h -> distanceKm(h, target)))
                        .orElseThrow();
            } else {
                // fail-safe: 200km 내에서 목적지까지 거리가 가장 가까운 후보
                next = candidates.stream()
                        .min(Comparator.comparingDouble(h -> distanceKm(h, target)))
                        .orElseThrow();
            }

            double segKm = distanceKm(current, next);
            legs.add(new RouteLeg(current.getHubId(), next.getHubId(), round2(segKm), (int)Math.round(segKm * KM_TO_MIN)));
            visited.add(next.getHubId());
            current = next;
        }

        // 마지막 구간(current -> target) 연결
        double lastKm = distanceKm(current, target);
        legs.add(new RouteLeg(current.getHubId(), target.getHubId(), round2(lastKm), (int)Math.round(lastKm * KM_TO_MIN)));

        double totalKm = legs.stream().mapToDouble(RouteLeg::distanceKm).sum();
        int totalMin = legs.stream().mapToInt(RouteLeg::estimatedMinutes).sum();
        return new RoutePlanResponse(sourceId, targetId, round2(totalKm), totalMin, legs);
    }

    private <T> T optionalOrThrow(T v, String msg) {
        if (v == null) throw new IllegalArgumentException(msg);
        return v;
    }

    // Haversine
    private double distanceKm(Hub a, Hub b) {
        return distanceKm(a.getLatitude(), a.getLongitude(), b.getLatitude(), b.getLongitude());
    }

    private double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double s = Math.sin(dLat/2) * Math.sin(dLat/2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(s), Math.sqrt(1-s));
        return R * c;
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
