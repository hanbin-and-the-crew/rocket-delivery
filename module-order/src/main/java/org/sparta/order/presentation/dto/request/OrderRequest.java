package org.sparta.order.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.sparta.order.application.command.OrderCommand;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 주문 관련 Request DTO
 */
public class OrderRequest {

    @Schema(description = "주문 생성 요청")
    public record Create(
            @Schema(description = "공급업체 ID", example = "550e8400-e29b-41d4-a716-446655440001")
            @NotNull(message = "공급업체 ID는 필수입니다")
            UUID supplierCompanyId,

            @Schema(description = "공급업체 허브 ID", example = "550e8400-e29b-41d4-a716-446655440002")
            @NotNull(message = "공급업체 허브 ID는 필수입니다")
            UUID supplierHubId,

            @Schema(description = "수령업체 ID", example = "550e8400-e29b-41d4-a716-446655440003")
            @NotNull(message = "수령업체 ID는 필수입니다")
            UUID receiptCompanyId,

            @Schema(description = "수령업체 허브 ID", example = "550e8400-e29b-41d4-a716-446655440004")
            @NotNull(message = "수령업체 허브 ID는 필수입니다")
            UUID receiptHubId,

            @Schema(description = "상품 ID", example = "550e8400-e29b-41d4-a716-446655440005")
            @NotNull(message = "상품 ID는 필수입니다")
            UUID productId,

            @Schema(description = "주문 수량", example = "10")
            @NotNull(message = "주문 수량은 필수입니다")
            @Min(value = 1, message = "주문 수량은 최소 1개 이상이어야 합니다")
            Integer quantity,

            @Schema(description = "상품 가격", example = "10000")
            @NotNull(message = "상품 가격은 필수입니다")
            @Min(value = 1, message = "상품 가격은 최소 1 이상이어야 합니다")
            Integer productPrice,

            @Schema(description = "배송지 주소", example = "서울특별시 강남구 테헤란로 123")
            @NotBlank(message = "배송지 주소는 필수입니다")
            String address,

            @Schema(description = "주문자 실명", example = "김손님")
            @NotBlank(message = "주문자 실명은 필수입니다")
            String userName,

            @Schema(description = "전화번호", example = "010-1111-2222")
            @NotBlank(message = "전화번호는 필수입니다")
            String userPhoneNumber,

            @Schema(description = "slack 아이디", example = "12@1234.com")
            @NotBlank(message = "slack 아이디는 필수입니다")
            String slackId,

            @Schema(description = "납품 기한", example = "2025-12-31T23:59:59")
            @NotNull(message = "납품 기한은 필수입니다")
            LocalDateTime dueAt,

            @Schema(description = "요청사항", example = "빠른 배송 부탁드립니다")
            String requestMemo,

            @Schema(description = "사용할 포인트", example = "1000")
            Integer requestPoint,

            @Schema(description = "결제 수단", example = "CARD")
            String methodType,

            @Schema(description = "결제 요청 업체", example = "TOSS")
            String pgProvider,

            @Schema(description = "결제 화폐", example = "KRW")
            String currency,

            @Schema(description = "사용할 쿠폰 ID", example = "COUPON-RES-456")
            String couponId
    ) {
    }


    @Schema(description = "납품 기한 변경 요청")
    public record ChangeDueAt(
            @Schema(description = "변경할 납품 기한", example = "2025-12-31T23:59:59")
            @NotNull(message = "납품 기한은 필수입니다")
            LocalDateTime dueAt
    ) {
    }

    @Schema(description = "요청사항 변경 요청")
    public record ChangeMemo(
            @Schema(description = "변경할 요청사항", example = "배송 전 연락 부탁드립니다")
            String requestedMemo
    ) {
    }

    @Schema(description = "주소 변경 요청")
    public record ChangeAddress(
            @Schema(description = "변경할 주소", example = "서울시 강동구 천호빌딩 101-1")
            String addressSnapshot
    ) {
    }

    @Schema(description = "주문 취소 요청")
    public record Cancel(

            @Schema(description = "취소할 주문 id", example = "550e8400-e29b-41d4-a716-446655440005")
            @NotNull(message = "취소할 주문 id는 필수입니다")
            UUID orderId,

            @Schema(description = "취소 사유 코드", example = "CUSTOMER_REQUEST")
            @NotNull(message = "취소 사유 코드는 필수입니다")
            String reasonCode,

            @Schema(description = "취소 사유 상세", example = "고객 요청으로 취소합니다")
            @NotBlank(message = "취소 사유 상세는 필수입니다")
            String reasonMemo
    ) {
    }

    @Schema(description = "주문 배송출발/출고 처리 요청")
    public record ShipOrder(
            @Schema(description = "처리할 주문 id", example = "550e8400-e29b-41d4-a716-446655440005")
            @NotNull(message = "처리할 주문 id는 필수입니다.")
            UUID orderId
    ) {
    }

    @Schema(description = "주문 배송완료 처리 요청")
    public record DeliverOrder(
            @Schema(description = "처리할 주문 id", example = "550e8400-e29b-41d4-a716-446655440005")
            @NotNull(message = "처리할 주문 id는 필수입니다.")
            UUID orderId
    ) {
    }

    @Schema(description = "주문 삭제 처리 요청")
    public record DeleteOrder(
            @Schema(description = "처리할 주문 id", example = "550e8400-e29b-41d4-a716-446655440005")
            @NotNull(message = "처리할 주문 id는 필수입니다.")
            UUID orderId
    ) {
    }


}