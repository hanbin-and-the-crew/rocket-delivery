package org.sparta.user.presentation.ApiSpec;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.sparta.common.api.ApiResponse;
import org.sparta.user.presentation.dto.request.PointRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@Tag(name = "Point API",  description = "포인트 관리" )
public interface PointApiSpec {

    @Operation(
            summary = "포인트 예약 요청",
            description = "결제 진행시 요청한 포인트만큼 예약을 요청하고 결제가 완료되면 이벤트를 받아 포인트 소모를 확정합니다."
    )
    @PostMapping("/reserve")
    ApiResponse<Object> reservePoint(
            @Valid @RequestBody PointRequest.Reserve request
    );

    @Operation(
            summary = "포인트 결제 확정",
            description = "예약하였던 포인트를 확정 처리합니다."
    )
    @PostMapping("/confirm")
    void confirmPoint(
            @Valid @RequestBody PointRequest.Confirm request
    );

    @Operation(
            summary = "포인트 확인",
            description = "사용자의 현재 포인트를 확인합니다."
    )
    @GetMapping("/{userId}")
    ApiResponse<Object> getPoint(
            @PathVariable UUID userId
    );
}