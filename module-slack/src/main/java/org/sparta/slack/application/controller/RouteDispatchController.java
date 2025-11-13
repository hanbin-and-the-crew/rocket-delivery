package org.sparta.slack.application.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.sparta.slack.application.dto.route.DailyDispatchResult;
import org.sparta.slack.application.dto.route.DispatchRequest;
import org.sparta.slack.application.dto.route.RouteRegistrationRequest;
import org.sparta.slack.application.dto.route.RouteResponse;
import org.sparta.slack.application.service.route.CompanyDeliveryRouteCommandService;
import org.sparta.slack.application.service.route.CompanyDeliveryRouteQueryService;
import org.sparta.slack.application.service.route.DailyRouteDispatchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 일일 배송 루트 알림(매일 06시 스케줄링)을 위한 경로 등록/조회/발송 컨트롤러.
 * 대상: 업체 배송 담당자(최종 배달원)에게 AI+네이버 API로 계산한 최적 방문 순서를 전달한다.
 * 시점/목적: 외부 시스템이 배송 경로 데이터를 적재하거나 특정 날짜만 재발송하도록 수동 호출할 때 사용하며,
 * 예시 메시지는 "오늘 배송지: A업체(9:00) → B업체(10:30) → C업체(11:00)" 형태이다.
 */
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
