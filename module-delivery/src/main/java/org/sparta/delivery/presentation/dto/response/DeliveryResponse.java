package org.sparta.delivery.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.sparta.delivery.domain.entity.Delivery;
import org.sparta.delivery.domain.enumeration.DeliveryStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class DeliveryResponse {

    @Schema(description = "배송 상세 응답")
    public record Detail(

            @Schema(description = "배송 ID")
            UUID id,

            @Schema(description = "주문 ID")
            UUID orderId,

            @Schema(description = "고객 ID")
            UUID customerId,

            @Schema(description = "공급 업체 ID")
            UUID supplierCompanyId,

            @Schema(description = "공급 허브 ID")
            UUID supplierHubId,

            @Schema(description = "수령 업체 ID")
            UUID receiveCompanyId,

            @Schema(description = "수령 허브 ID")
            UUID receiveHubId,

            @Schema(description = "배송지 주소")
            String address,

            @Schema(description = "수령인 이름")
            String receiverName,

            @Schema(description = "수령인 슬랙 ID")
            String receiverSlackId,

            @Schema(description = "수령인 전화번호")
            String receiverPhone,

            @Schema(description = "납품 기한")
            LocalDateTime dueAt,

            @Schema(description = "요청 사항")
            String requestedMemo,

            @Schema(description = "배송 상태")
            DeliveryStatus status,

            @Schema(description = "현재 허브 로그 시퀀스")
            Integer currentLogSeq,

            @Schema(description = "전체 허브 로그 시퀀스 개수")
            Integer totalLogSeq,

            @Schema(description = "허브 배송 담당자 ID")
            UUID hubDeliveryManId,

            @Schema(description = "업체 배송 담당자 ID")
            UUID companyDeliveryManId,

            @Schema(description = "생성 일시")
            LocalDateTime createdAt,

            @Schema(description = "수정 일시")
            LocalDateTime updatedAt
    ) {
        public static Detail from(Delivery d) {
            return new Detail(
                    d.getId(),
                    d.getOrderId(),
                    d.getCustomerId(),
                    d.getSupplierCompanyId(),
                    d.getSupplierHubId(),
                    d.getReceiveCompanyId(),
                    d.getReceiveHubId(),
                    d.getAddress(),
                    d.getReceiverName(),
                    d.getReceiverSlackId(),
                    d.getReceiverPhone(),
                    d.getDueAt(),
                    d.getRequestedMemo(),
                    d.getStatus(),
                    d.getCurrentLogSeq(),
                    d.getTotalLogSeq(),
                    d.getHubDeliveryManId(),
                    d.getCompanyDeliveryManId(),
                    d.getCreatedAt(),
                    d.getUpdatedAt()
            );
        }
    }

    @Schema(description = "배송 목록 요약 응답")
    public record Summary(

            @Schema(description = "배송 ID")
            UUID id,

            @Schema(description = "주문 ID")
            UUID orderId,

            @Schema(description = "공급 허브 ID")
            UUID supplierHubId,

            @Schema(description = "수령 허브 ID")
            UUID receiveHubId,

            @Schema(description = "배송 상태")
            DeliveryStatus status,

            @Schema(description = "현재 허브 로그 시퀀스")
            Integer currentLogSeq,

            @Schema(description = "전체 허브 로그 시퀀스 개수")
            Integer totalLogSeq
    ) {
        public static Summary from(Delivery d) {
            return new Summary(
                    d.getId(),
                    d.getOrderId(),
                    d.getSupplierHubId(),
                    d.getReceiveHubId(),
                    d.getStatus(),
                    d.getCurrentLogSeq(),
                    d.getTotalLogSeq()
            );
        }
    }

    @Schema(description = "배송 페이징 결과")
    public record PageResult(

            @Schema(description = "내용 목록")
            List<Summary> content,

            @Schema(description = "전체 개수")
            long totalElements,

            @Schema(description = "전체 페이지 수")
            int totalPages
    ) { }
}
