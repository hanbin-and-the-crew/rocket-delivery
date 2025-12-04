package org.sparta.order.presentation.dto;

import org.sparta.order.application.command.OrderCommand;
import org.sparta.order.presentation.dto.request.OrderRequest;
import org.springframework.stereotype.Component;

@Component
public class OrderMapper {

    /** 주문 생성 */
    public OrderCommand.Create toCommand(OrderRequest.Create req) {
        return new OrderCommand.Create(
                req.supplierCompanyId(),
                req.supplierHubId(),
                req.receiptCompanyId(),
                req.receiptHubId(),
                req.productId(),
                req.quantity(),
                req.productPrice(),
                req.address(),
                req.userName(),
                req.userPhoneNumber(),
                req.slackId(),
                req.dueAt(),
                req.requestMemo()
        );
    }

    /** 납품 기한 변경 */
    public OrderCommand.ChangeDueAt toCommand(OrderRequest.ChangeDueAt req) {
        return new OrderCommand.ChangeDueAt(
                req.dueAt()
        );
    }

    /** 요청 메모 변경 */
    public OrderCommand.changeRequestMemo toCommand(OrderRequest.ChangeMemo req) {
        return new OrderCommand.changeRequestMemo(
                req.requestedMemo()   // DTO 명: requestedMemo → Command 명: memo
        );
    }

    /** 주소 변경 */
    public OrderCommand.ChangeAddress toCommand(OrderRequest.ChangeAddress req) {
        return new OrderCommand.ChangeAddress(
                req.addressSnapshot() // DTO 명: addressSnapshot → Command 명: address
        );
    }

    /** 주문 취소 */
    public OrderCommand.Cancel toCommand(OrderRequest.Cancel req) {
        return new OrderCommand.Cancel(
                req.orderId(),
                req.reasonCode(),
                req.reasonMemo()
        );
    }

    /** 배송 출발/출고 처리 */
    public OrderCommand.ShipOrder toCommand(OrderRequest.ShipOrder req) {
        return new OrderCommand.ShipOrder(
                req.orderId()
        );
    }

    /** 배송 완료 처리 */
    public OrderCommand.DeliverOrder toCommand(OrderRequest.DeliverOrder req) {
        return new OrderCommand.DeliverOrder(
                req.orderId()
        );
    }

    /** 주문 삭제 처리 */
    public OrderCommand.DeleteOrder toCommand(OrderRequest.DeleteOrder req) {
        return new OrderCommand.DeleteOrder(
                req.orderId()
        );
    }
}
