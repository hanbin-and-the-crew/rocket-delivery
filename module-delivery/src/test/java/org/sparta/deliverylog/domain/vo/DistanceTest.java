//package org.sparta.deliverylog.domain.vo;
//
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//
//import static org.assertj.core.api.Assertions.*;
//
//@DisplayName("Distance VO 테스트")
//class DistanceTest {
//
//    @Test
//    @DisplayName("Distance 생성 성공")
//    void create_Success() {
//        // given
//        Double value = 10.5;
//
//        // when
//        Distance distance = Distance.of(value);
//
//        // then
//        assertThat(distance.getValue()).isEqualTo(value);
//    }
//
//    @Test
//    @DisplayName("Distance 생성 실패 - 음수")
//    void create_Fail_Negative() {
//        // given
//        Double value = -10.0;
//
//        // when & then
//        assertThatThrownBy(() -> Distance.of(value))
//                .isInstanceOf(IllegalArgumentException.class)
//                .hasMessage("거리는 음수일 수 없습니다");
//    }
//
//    @Test
//    @DisplayName("Distance null 처리")
//    void create_Null() {
//        // when
//        Distance distance = Distance.of(null);
//
//        // then
//        assertThat(distance.getValue()).isNull();
//    }
//
//    @Test
//    @DisplayName("Distance 포맷팅")
//    void format_Success() {
//        // given
//        Distance distance = Distance.of(10.5);
//
//        // when
//        String formatted = distance.format();
//
//        // then
//        assertThat(formatted).isEqualTo("10.50 km");
//    }
//}
