//package org.sparta.order.support.fixtures;
//
//import org.sparta.order.application.dto.request.OrderRequest;
//
//import java.time.LocalDateTime;
//import java.util.UUID;
//
//public final class OrderFixture {
//
//    public OrderFixture() {
//    }
//
//    public static OrderRequest.Create createValidRequest() {
//        return new OrderRequest.Create(
//                UUID.randomUUID(), // supplierId
//                UUID.randomUUID(), // supplierCompanyId
//                UUID.randomUUID(), // supplierHubId
//                UUID.randomUUID(), // receiptCompanyId
//                UUID.randomUUID(), // receiptHubId
//                UUID.randomUUID(), // productId
//                3, // quantity
//                "서울특별시 강남구 테헤란로 123", // deliveryAddress
//                "홍길동", // userName
//                "010-1111-2222", // userPhoneNumber
//                "test@sample.com", // slackId
//                LocalDateTime.now().plusDays(5), // dueAt
//                "정상 주문 요청" // requestedMemo
//        );
//    }
//
//    public static OrderRequest.Create createInvalidRequest() {
//        return new OrderRequest.Create(
//                UUID.randomUUID(), // supplierId
//                UUID.randomUUID(), // supplierCompanyId
//                UUID.randomUUID(), // supplierHubId
//                UUID.randomUUID(), // receiptCompanyId
//                UUID.randomUUID(), // receiptHubId
//                UUID.randomUUID(), // productId
//                -5, // 유효하지 않은 quantity
//                "서울특별시 강남구 테헤란로 123",
//                "홍길동",
//                "010-1111-2222",
//                "test@sample.com",
//                LocalDateTime.now().plusDays(5),
//                "잘못된 주문 요청"
//        );
//    }
//}
