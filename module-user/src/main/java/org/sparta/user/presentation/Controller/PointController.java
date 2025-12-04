package org.sparta.user.presentation.Controller;

import jakarta.validation.Valid;
import org.sparta.common.api.ApiResponse;
import org.sparta.user.application.command.PointCommand;
import org.sparta.user.application.service.PointService;
import org.sparta.user.presentation.ApiSpec.PointApiSpec;
import org.sparta.user.presentation.dto.PointMapper;
import org.sparta.user.presentation.dto.request.PointRequest;
import org.sparta.user.presentation.dto.response.PointResponse;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users/point")
public class PointController implements PointApiSpec {
    private final PointService pointService;
    private final PointMapper pointMapper;

    public PointController(PointService pointService, PointMapper pointMapper) {
        this.pointService = pointService;
        this.pointMapper = pointMapper;
    }

    @Override
    @PostMapping("/reserve")
    public ApiResponse<Object> reservePoint(
            @Valid @RequestBody PointRequest.Reserve request
    ) {
        PointCommand.ReservePoint command = pointMapper.toCommand(request);
        PointResponse.PointReservationResult response = pointService.reservePoints(command);
        return ApiResponse.success(response);
    }

    @Override
    @PostMapping("/confirm")
    public void confirmPoint(
            @Valid @RequestBody PointRequest.Confirm request
    ) {
        PointCommand.ConfirmPoint command = pointMapper.toCommand(request);
        pointService.confirmPointUsage(command);
    }

    @Override
    @GetMapping("/{userId}")
    public ApiResponse<Object> getPoint(
            @PathVariable UUID userId
    ) {
        PointResponse.PointSummary response = pointService.getPoint(userId);
        return ApiResponse.success(response);
    }
}
