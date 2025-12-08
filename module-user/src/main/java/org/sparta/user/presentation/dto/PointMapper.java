package org.sparta.user.presentation.dto;

import org.sparta.user.application.command.PointCommand;
import org.sparta.user.infrastructure.event.OrderApprovedEvent;
import org.sparta.user.presentation.dto.request.PointRequest;
import org.springframework.stereotype.Component;

@Component
public class PointMapper {

    public PointCommand.ReservePoint toCommand(PointRequest.Reserve req) {
        return new PointCommand.ReservePoint(
                req.userId(),
                req.orderId(),
                req.orderAmount(),
                req.requestPoint()
        );
    }

    /**
     * 포인트 사용 확정 REST API -> Command
     */
    public PointCommand.ConfirmPoint toCommand(PointRequest.Confirm req) {
        return new PointCommand.ConfirmPoint(
                req.orderId()
        );
    }

    /**
     * 포인트 사용 확정 Kafka Event -> Command
     */
    public PointCommand.ConfirmPoint toCommand(OrderApprovedEvent event) {
        return new PointCommand.ConfirmPoint(
                event.orderId()
        );
    }
}