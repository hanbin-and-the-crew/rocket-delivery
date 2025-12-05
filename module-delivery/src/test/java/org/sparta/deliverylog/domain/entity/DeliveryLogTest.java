//package org.sparta.deliverylog.domain.entity;
//
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.sparta.deliverylog.domain.enumeration.DeliveryRouteStatus;
//
//import java.util.UUID;
//
//import static org.assertj.core.api.Assertions.*;
//
//@DisplayName("DeliveryLog 엔티티 테스트")
//class DeliveryLogTest {
//
//    @Test
//    @DisplayName("배송 경로 생성 성공")
//    void createDeliveryLog_Success() {
//        // given
//        UUID deliveryId = UUID.randomUUID();
//        UUID departureHubId = UUID.randomUUID();
//        UUID destinationHubId = UUID.randomUUID();
//        Integer hubSequence = 1;
//        Double expectedDistance = 10.5;
//        Integer expectedTime = 30;
//
//        // when
//        DeliveryLog deliveryLog = DeliveryLog.create(
//                deliveryId,
//                hubSequence,
//                departureHubId,
//                destinationHubId,
//                expectedDistance,
//                expectedTime
//        );
//
//        // then
//        assertThat(deliveryLog.getDeliveryId()).isEqualTo(deliveryId);
//        assertThat(deliveryLog.getHubSequence()).isEqualTo(hubSequence);
//        assertThat(deliveryLog.getDepartureHubId()).isEqualTo(departureHubId);
//        assertThat(deliveryLog.getDestinationHubId()).isEqualTo(destinationHubId);
//        assertThat(deliveryLog.getExpectedDistance().getValue()).isEqualTo(expectedDistance);
//        assertThat(deliveryLog.getExpectedTime().getValue()).isEqualTo(expectedTime);
//        assertThat(deliveryLog.getDeliveryStatus()).isEqualTo(DeliveryRouteStatus.WAITING);
//    }
//
//    @Test
//    @DisplayName("배송 담당자 배정 성공")
//    void assignDeliveryMan_Success() {
//        // given
//        DeliveryLog deliveryLog = createTestDeliveryLog();
//        UUID deliveryManId = UUID.randomUUID();
//
//        // when
//        deliveryLog.assignDeliveryMan(deliveryManId);
//
//        // then
//        assertThat(deliveryLog.getDeliveryManId()).isEqualTo(deliveryManId);
//    }
//
//    @Test
//    @DisplayName("배송 담당자 배정 실패 - null")
//    void assignDeliveryMan_Fail_Null() {
//        // given
//        DeliveryLog deliveryLog = createTestDeliveryLog();
//
//        // when & then
//        assertThatThrownBy(() -> deliveryLog.assignDeliveryMan(null))
//                .isInstanceOf(IllegalArgumentException.class)
//                .hasMessage("배송 담당자 ID는 필수입니다");
//    }
//
//    @Test
//    @DisplayName("배송 시작 성공")
//    void startDelivery_Success() {
//        // given
//        DeliveryLog deliveryLog = createTestDeliveryLog();
//        deliveryLog.assignDeliveryMan(UUID.randomUUID());
//
//        // when
//        deliveryLog.startDelivery();
//
//        // then
//        assertThat(deliveryLog.getDeliveryStatus()).isEqualTo(DeliveryRouteStatus.MOVING);
//    }
//
//    @Test
//    @DisplayName("배송 시작 실패 - 배송 담당자 미배정")
//    void startDelivery_Fail_NoDeliveryMan() {
//        // given
//        DeliveryLog deliveryLog = createTestDeliveryLog();
//
//        // when & then
//        assertThatThrownBy(() -> deliveryLog.startDelivery())
//                .isInstanceOf(IllegalStateException.class)
//                .hasMessage("배송 담당자가 배정되지 않았습니다");
//    }
//
//    @Test
//    @DisplayName("배송 시작 실패 - 잘못된 상태")
//    void startDelivery_Fail_InvalidStatus() {
//        // given
//        DeliveryLog deliveryLog = createTestDeliveryLog();
//        deliveryLog.assignDeliveryMan(UUID.randomUUID());
//        deliveryLog.startDelivery();
//
//        // when & then
//        assertThatThrownBy(() -> deliveryLog.startDelivery())
//                .isInstanceOf(IllegalStateException.class)
//                .hasMessage("대기 중 상태에서만 배송을 시작할 수 있습니다");
//    }
//
//    @Test
//    @DisplayName("배송 완료 성공")
//    void completeDelivery_Success() {
//        // given
//        DeliveryLog deliveryLog = createTestDeliveryLog();
//        deliveryLog.assignDeliveryMan(UUID.randomUUID());
//        deliveryLog.startDelivery();
//        Double actualDistance = 11.0;
//        Integer actualTime = 32;
//
//        // when
//        deliveryLog.completeDelivery(actualDistance, actualTime);
//
//        // then
//        assertThat(deliveryLog.getDeliveryStatus()).isEqualTo(DeliveryRouteStatus.COMPLETED);
//        assertThat(deliveryLog.getActualDistance().getValue()).isEqualTo(actualDistance);
//        assertThat(deliveryLog.getActualTime().getValue()).isEqualTo(actualTime);
//    }
//
//    @Test
//    @DisplayName("배송 완료 실패 - 잘못된 상태")
//    void completeDelivery_Fail_InvalidStatus() {
//        // given
//        DeliveryLog deliveryLog = createTestDeliveryLog();
//
//        // when & then
//        assertThatThrownBy(() -> deliveryLog.completeDelivery(10.0, 30))
//                .isInstanceOf(IllegalStateException.class)
//                .hasMessage("이동 중 상태에서만 배송을 완료할 수 있습니다");
//    }
//
//    @Test
//    @DisplayName("배송 취소 성공")
//    void cancel_Success() {
//        // given
//        DeliveryLog deliveryLog = createTestDeliveryLog();
//
//        // when
//        deliveryLog.cancel();
//
//        // then
//        assertThat(deliveryLog.getDeliveryStatus()).isEqualTo(DeliveryRouteStatus.CANCELED);
//    }
//
//    @Test
//    @DisplayName("배송 취소 실패 - 이미 완료됨")
//    void cancel_Fail_AlreadyCompleted() {
//        // given
//        DeliveryLog deliveryLog = createTestDeliveryLog();
//        deliveryLog.assignDeliveryMan(UUID.randomUUID());
//        deliveryLog.startDelivery();
//        deliveryLog.completeDelivery(10.0, 30);
//
//        // when & then
//        assertThatThrownBy(() -> deliveryLog.cancel())
//                .isInstanceOf(IllegalStateException.class)
//                .hasMessage("완료된 배송은 취소할 수 없습니다");
//    }
//
//    @Test
//    @DisplayName("예상 값 수정 성공")
//    void updateExpectedValues_Success() {
//        // given
//        DeliveryLog deliveryLog = createTestDeliveryLog();
//        Double newDistance = 15.0;
//        Integer newTime = 45;
//
//        // when
//        deliveryLog.updateExpectedValues(newDistance, newTime);
//
//        // then
//        assertThat(deliveryLog.getExpectedDistance().getValue()).isEqualTo(newDistance);
//        assertThat(deliveryLog.getExpectedTime().getValue()).isEqualTo(newTime);
//    }
//
//    @Test
//    @DisplayName("예상 값 수정 실패 - 잘못된 상태")
//    void updateExpectedValues_Fail_InvalidStatus() {
//        // given
//        DeliveryLog deliveryLog = createTestDeliveryLog();
//        deliveryLog.assignDeliveryMan(UUID.randomUUID());
//        deliveryLog.startDelivery();
//
//        // when & then
//        assertThatThrownBy(() -> deliveryLog.updateExpectedValues(15.0, 45))
//                .isInstanceOf(IllegalStateException.class)
//                .hasMessage("대기 중 상태에서만 예상 값을 수정할 수 있습니다");
//    }
//
//    @Test
//    @DisplayName("배송 담당자 변경 성공")
//    void changeDeliveryMan_Success() {
//        // given
//        DeliveryLog deliveryLog = createTestDeliveryLog();
//        UUID oldDeliveryManId = UUID.randomUUID();
//        UUID newDeliveryManId = UUID.randomUUID();
//        deliveryLog.assignDeliveryMan(oldDeliveryManId);
//
//        // when
//        deliveryLog.changeDeliveryMan(newDeliveryManId);
//
//        // then
//        assertThat(deliveryLog.getDeliveryManId()).isEqualTo(newDeliveryManId);
//    }
//
//    @Test
//    @DisplayName("배송 담당자 변경 실패 - 완료된 배송")
//    void changeDeliveryMan_Fail_Completed() {
//        // given
//        DeliveryLog deliveryLog = createTestDeliveryLog();
//        deliveryLog.assignDeliveryMan(UUID.randomUUID());
//        deliveryLog.startDelivery();
//        deliveryLog.completeDelivery(10.0, 30);
//
//        // when & then
//        assertThatThrownBy(() -> deliveryLog.changeDeliveryMan(UUID.randomUUID()))
//                .isInstanceOf(IllegalStateException.class)
//                .hasMessage("완료된 배송은 담당자를 변경할 수 없습니다");
//    }
//
//    // ========== Helper Methods ==========
//
//    private DeliveryLog createTestDeliveryLog() {
//        return DeliveryLog.create(
//                UUID.randomUUID(),
//                1,
//                UUID.randomUUID(),
//                UUID.randomUUID(),
//                10.0,
//                30
//        );
//    }
//}
