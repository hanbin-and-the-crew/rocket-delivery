package org.sparta.user.presentation.ApiSpec;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.sparta.common.api.ApiResponse;
import org.sparta.user.presentation.dto.request.PointRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Point API",  description = "포인트 관리" )
public interface PointApiSpec {

    @Operation(
            summary = "Point 예약 요청",
            description = "결제 진행시 요청한 포인트만큼 예약을 요청하고 결제가 완료되면 이벤트를 받아 포인트 소모를 확정합니다."
    )
    @PostMapping("/reserve")
    ApiResponse<Object> reservePoint(
            @Valid @RequestBody PointRequest.Reserve request
    );
}