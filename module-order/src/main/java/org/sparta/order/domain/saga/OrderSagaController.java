//package org.sparta.order.domain.saga;
//
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import java.io.Serializable;
//import java.util.Map;
//import java.util.Optional;
//import java.util.UUID;
//
//@RestController
//@RequestMapping("/api/order/saga")
//@RequiredArgsConstructor
//public class OrderSagaController {
//
//    private final SagaStateRepository repo;
//
//    @GetMapping("/{orderId}")
//    public ResponseEntity<Map<String, Object>> getSagaStatus(@PathVariable UUID orderId) {
//        Optional<SagaState> optionalState = repo.findByOrderId(orderId);
//
//        if (optionalState.isPresent()) {
//            SagaState state = optionalState.get();
//            Map<String, Object> statusMap = Map.of(
//                    "orderId", orderId,
//                    "orderStatus", state.getOrderStatus(),
//                    "paymentStatus", state.getPaymentStatus(),
//                    "deliveryStatus", state.getDeliveryStatus(),
//                    "overallStatus", state.getOverallStatus()
//            );
//            return ResponseEntity.ok(statusMap);
//        }
//        return ResponseEntity.notFound().build();
//    }
//
//}
//
//
