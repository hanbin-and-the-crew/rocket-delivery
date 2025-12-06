package org.sparta.deliveryman.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.sparta.common.api.ApiResponse;
import org.sparta.deliveryman.application.dto.DeliveryManRequest;
import org.sparta.deliveryman.application.dto.DeliveryManResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "DeliveryMan", description = "배송 담당자 API")
@RequestMapping("/api/deliveryman")
public interface DeliveryManApiSpec {

    @Operation(summary = "배송 담당자 등록")
    @PostMapping
    ResponseEntity<ApiResponse<DeliveryManResponse.Detail>> create(@Valid @RequestBody DeliveryManRequest.Create request);

    @Operation(summary = "배송 담당자 상세 조회")
    @GetMapping("/{id}")
    ResponseEntity<ApiResponse<DeliveryManResponse.Detail>> findById(@Parameter(description = "배송 담당자 ID") @PathVariable UUID id);

    @Operation(summary = "배송 담당자 목록 조회")
    @GetMapping
    ResponseEntity<ApiResponse<Page<DeliveryManResponse.Summary>>> search(Pageable pageable);

    @Operation(summary = "배송 담당자 수정")
    @PutMapping("/{id}")
    ResponseEntity<ApiResponse<DeliveryManResponse.Detail>> update(@Parameter(description = "배송 담당자 ID") @PathVariable UUID id,
                                                                   @Valid @RequestBody DeliveryManRequest.Update request);

    @Operation(summary = "배송 담당자 삭제")
    @DeleteMapping("/{id}")
    ResponseEntity<ApiResponse<Object>> delete(@Parameter(description = "배송 담당자 ID") @PathVariable UUID id);
}
