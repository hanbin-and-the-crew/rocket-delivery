package org.sparta.user.presentation.dto;

import org.sparta.user.application.command.PointCommand;
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

    public PointCommand.ConfirmPoint toCommand(PointRequest.Confirm req) {
        return new PointCommand.ConfirmPoint(
                req.orderId()
        );
    }
}