package org.sparta.hub.presentation.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class HubRouteSummaryResponse {
    private UUID sourceHubId;
    private UUID targetHubId;
    private int totalDistance;
    private int totalDuration;
    private List<String> intermediateHubs;
}
