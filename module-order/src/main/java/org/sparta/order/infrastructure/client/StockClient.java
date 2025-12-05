package org.sparta.order.infrastructure.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@FeignClient(
        name = "product-service"
//        url = "http://localhost:19506"
)
public interface StockClient {

    @PostMapping("/product/stocks/reserve")
    StockReserveResponse reserveStock(@RequestBody StockReserveRequest request);

    record StockReserveRequest(
            UUID productId,
            String reservationKey,  // orderId를 String으로 변환한거
            Integer quantity
    ){}


    record StockReserveResponse(
            UUID reservationId, // OrderCreatedEvent에서 사용
            UUID stockId,
            String reservationKey,
            Integer reservedQuantity,
            String status // "RESERVED"
    ) {}

}
