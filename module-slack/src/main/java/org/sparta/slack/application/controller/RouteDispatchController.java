package org.sparta.slack.application.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.sparta.slack.application.dto.*;
import org.sparta.slack.application.service.CompanyDeliveryRouteCommandService;
import org.sparta.slack.application.service.CompanyDeliveryRouteQueryService;
import org.sparta.slack.application.service.DailyRouteDispatchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/slack/routes")
@RequiredArgsConstructor
public class RouteDispatchController {

    private final CompanyDeliveryRouteCommandService commandService;
    private final CompanyDeliveryRouteQueryService queryService;
    private final DailyRouteDispatchService dailyRouteDispatchService;

    @PostMapping
    public ResponseEntity<RouteResponse> register(@Valid @RequestBody RouteRegistrationRequest request) {
        var route = commandService.register(request.toCommand());
        return ResponseEntity.ok(RouteResponse.from(route));
    }

    @GetMapping
    public ResponseEntity<List<RouteResponse>> list(@RequestParam LocalDate date) {
        List<RouteResponse> responses = queryService.findByDate(date).stream()
                .map(RouteResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/dispatch")
    public ResponseEntity<List<DailyDispatchResult>> dispatch(@RequestBody(required = false) DispatchRequest request) {
        LocalDate targetDate = request != null && request.date() != null
                ? request.date()
                : LocalDate.now();
        List<DailyDispatchResult> results = dailyRouteDispatchService.dispatch(targetDate);
        return ResponseEntity.ok(results);
    }
}
