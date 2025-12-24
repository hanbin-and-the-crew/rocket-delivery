//package org.sparta.delivery.fixture;
//
//import org.sparta.delivery.domain.entity.Delivery;
//import org.sparta.delivery.domain.enumeration.DeliveryStatus;
//
//import java.util.UUID;
//
///**
// * Delivery 테스트용 Fixture 클래스
// * 테스트에서 Delivery 엔티티를 쉽게 생성할 수 있도록 다양한 팩토리 메서드 제공
// */
//public class DeliveryFixture {
//
//    // 기본 데이터
//    private static final String DEFAULT_ADDRESS = "서울특별시 강남구 테헤란로 123";
//    private static final String DEFAULT_RECIPIENT_NAME = "홍길동";
//    private static final String DEFAULT_RECIPIENT_SLACK_ID = "@홍길동";
//
//    /**
//     * 기본 배송 생성 (모든 값 랜덤)
//     */
//    public static Delivery createDelivery() {
//        return Delivery.create(
//                UUID.randomUUID(),
//                UUID.randomUUID(),
//                UUID.randomUUID(),
//                DEFAULT_ADDRESS,
//                DEFAULT_RECIPIENT_NAME,
//                DEFAULT_RECIPIENT_SLACK_ID
//        );
//    }
//
//    /**
//     * 특정 주문 ID를 가진 배송 생성
//     */
//    public static Delivery createDelivery(UUID orderId) {
//        return Delivery.create(
//                orderId,
//                UUID.randomUUID(),
//                UUID.randomUUID(),
//                DEFAULT_ADDRESS,
//                DEFAULT_RECIPIENT_NAME,
//                DEFAULT_RECIPIENT_SLACK_ID
//        );
//    }
//
//    /**
//     * 특정 허브 ID를 가진 배송 생성
//     */
//    public static Delivery createDeliveryWithHub(UUID departureHubId, UUID destinationHubId) {
//        return Delivery.create(
//                UUID.randomUUID(),
//                departureHubId,
//                destinationHubId,
//                DEFAULT_ADDRESS,
//                DEFAULT_RECIPIENT_NAME,
//                DEFAULT_RECIPIENT_SLACK_ID
//        );
//    }
//
//    /**
//     * 특정 수령인 정보를 가진 배송 생성
//     */
//    public static Delivery createDeliveryWithRecipient(String recipientName, String recipientSlackId) {
//        return Delivery.create(
//                UUID.randomUUID(),
//                UUID.randomUUID(),
//                UUID.randomUUID(),
//                DEFAULT_ADDRESS,
//                recipientName,
//                recipientSlackId
//        );
//    }
//
//    /**
//     * 특정 주소를 가진 배송 생성
//     */
//    public static Delivery createDeliveryWithAddress(String deliveryAddress) {
//        return Delivery.create(
//                UUID.randomUUID(),
//                UUID.randomUUID(),
//                UUID.randomUUID(),
//                deliveryAddress,
//                DEFAULT_RECIPIENT_NAME,
//                DEFAULT_RECIPIENT_SLACK_ID
//        );
//    }
//
//    /**
//     * 특정 상태를 가진 배송 생성
//     */
//    public static Delivery createDeliveryWithStatus(DeliveryStatus status) {
//        Delivery delivery = createDelivery();
//
//        switch (status) {
//            case HUB_WAITING:
//                delivery.hubWaiting();
//                break;
//            case HUB_MOVING:
//                delivery.hubMoving();
//                break;
//            case DEST_HUB_ARRIVED:
//                delivery.arriveAtDestinationHub();
//                break;
//            case COMPANY_MOVING:
//                delivery.startCompanyMoving(UUID.randomUUID());
//                break;
//            case DELIVERED:
//                delivery.completeDelivery();
//                break;
//        }
//
//        return delivery;
//    }
//
//    /**
//     * 업체 배송 담당자가 배정된 배송 생성
//     */
//    public static Delivery createDeliveryWithCompanyMan(UUID companyDeliveryManId) {
//        Delivery delivery = createDelivery();
//        delivery.assignCompanyDeliveryMan(companyDeliveryManId);
//        return delivery;
//    }
//
//    /**
//     * 허브 배송 담당자가 배정된 배송 생성
//     */
//    public static Delivery createDeliveryWithHubMan(UUID hubDeliveryManId) {
//        Delivery delivery = createDelivery();
//        delivery.assignHubDeliveryMan(hubDeliveryManId);
//        return delivery;
//    }
//
//    /**
//     * 두 배송 담당자가 모두 배정된 배송 생성
//     */
//    public static Delivery createDeliveryWithBothDeliveryMan(UUID companyDeliveryManId, UUID hubDeliveryManId) {
//        Delivery delivery = createDelivery();
//        delivery.saveDeliveryMan(companyDeliveryManId, hubDeliveryManId);
//        return delivery;
//    }
//
//    /**
//     * 삭제된 배송 생성
//     */
//    public static Delivery createDeletedDelivery(UUID deletedBy) {
//        Delivery delivery = createDelivery();
//        delivery.delete(deletedBy);
//        return delivery;
//    }
//
//    /**
//     * 허브 이동 중인 배송 생성
//     */
//    public static Delivery createHubMovingDelivery() {
//        Delivery delivery = createDelivery();
//        delivery.hubMoving();
//        return delivery;
//    }
//
//    /**
//     * 목적지 허브 도착한 배송 생성
//     */
//    public static Delivery createDestinationHubArrivedDelivery() {
//        Delivery delivery = createDelivery();
//        delivery.hubMoving();
//        delivery.arriveAtDestinationHub();
//        return delivery;
//    }
//
//    /**
//     * 업체 배송 중인 배송 생성
//     */
//    public static Delivery createCompanyMovingDelivery(UUID companyDeliveryManId) {
//        Delivery delivery = createDelivery();
//        delivery.startCompanyMoving(companyDeliveryManId);
//        return delivery;
//    }
//
//    /**
//     * 배송 완료된 배송 생성
//     */
//    public static Delivery createDeliveredDelivery() {
//        Delivery delivery = createDelivery();
//        delivery.completeDelivery();
//        return delivery;
//    }
//
//    /**
//     * 모든 파라미터를 지정하여 배송 생성 (완전한 커스터마이징)
//     */
//    public static Delivery createDeliveryWithAllParams(
//            UUID orderId,
//            UUID departureHubId,
//            UUID destinationHubId,
//            String deliveryAddress,
//            String recipientName,
//            String recipientSlackId
//    ) {
//        return Delivery.create(
//                orderId,
//                departureHubId,
//                destinationHubId,
//                deliveryAddress,
//                recipientName,
//                recipientSlackId
//        );
//    }
//
//    /**
//     * Builder 스타일로 배송 생성
//     */
//    public static DeliveryBuilder builder() {
//        return new DeliveryBuilder();
//    }
//
//    /**
//     * Builder 클래스
//     */
//    public static class DeliveryBuilder {
//        private UUID orderId = UUID.randomUUID();
//        private UUID departureHubId = UUID.randomUUID();
//        private UUID destinationHubId = UUID.randomUUID();
//        private String deliveryAddress = DEFAULT_ADDRESS;
//        private String recipientName = DEFAULT_RECIPIENT_NAME;
//        private String recipientSlackId = DEFAULT_RECIPIENT_SLACK_ID;
//        private DeliveryStatus status = null;
//        private UUID companyDeliveryManId = null;
//        private UUID hubDeliveryManId = null;
//        private boolean isDeleted = false;
//        private UUID deletedBy = null;
//
//        public DeliveryBuilder orderId(UUID orderId) {
//            this.orderId = orderId;
//            return this;
//        }
//
//        public DeliveryBuilder departureHubId(UUID departureHubId) {
//            this.departureHubId = departureHubId;
//            return this;
//        }
//
//        public DeliveryBuilder destinationHubId(UUID destinationHubId) {
//            this.destinationHubId = destinationHubId;
//            return this;
//        }
//
//        public DeliveryBuilder deliveryAddress(String deliveryAddress) {
//            this.deliveryAddress = deliveryAddress;
//            return this;
//        }
//
//        public DeliveryBuilder recipientName(String recipientName) {
//            this.recipientName = recipientName;
//            return this;
//        }
//
//        public DeliveryBuilder recipientSlackId(String recipientSlackId) {
//            this.recipientSlackId = recipientSlackId;
//            return this;
//        }
//
//        public DeliveryBuilder status(DeliveryStatus status) {
//            this.status = status;
//            return this;
//        }
//
//        public DeliveryBuilder companyDeliveryManId(UUID companyDeliveryManId) {
//            this.companyDeliveryManId = companyDeliveryManId;
//            return this;
//        }
//
//        public DeliveryBuilder hubDeliveryManId(UUID hubDeliveryManId) {
//            this.hubDeliveryManId = hubDeliveryManId;
//            return this;
//        }
//
//        public DeliveryBuilder deleted(UUID deletedBy) {
//            this.isDeleted = true;
//            this.deletedBy = deletedBy;
//            return this;
//        }
//
//        public Delivery build() {
//            Delivery delivery = Delivery.create(
//                    orderId,
//                    departureHubId,
//                    destinationHubId,
//                    deliveryAddress,
//                    recipientName,
//                    recipientSlackId
//            );
//
//            // 상태 설정
//            if (status != null) {
//                switch (status) {
//                    case HUB_WAITING:
//                        delivery.hubWaiting();
//                        break;
//                    case HUB_MOVING:
//                        delivery.hubMoving();
//                        break;
//                    case DEST_HUB_ARRIVED:
//                        delivery.arriveAtDestinationHub();
//                        break;
//                    case COMPANY_MOVING:
//                        delivery.startCompanyMoving(companyDeliveryManId != null ? companyDeliveryManId : UUID.randomUUID());
//                        break;
//                    case DELIVERED:
//                        delivery.completeDelivery();
//                        break;
//                }
//            }
//
//            // 배송 담당자 배정
//            if (companyDeliveryManId != null || hubDeliveryManId != null) {
//                delivery.saveDeliveryMan(companyDeliveryManId, hubDeliveryManId);
//            }
//
//            // 삭제 처리
//            if (isDeleted) {
//                delivery.delete(deletedBy != null ? deletedBy : UUID.randomUUID());
//            }
//
//            return delivery;
//        }
//    }
//}
