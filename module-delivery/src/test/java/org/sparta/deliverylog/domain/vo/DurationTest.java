//package org.sparta.deliverylog.domain.vo;
//
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//
//import static org.assertj.core.api.Assertions.*;
//
//@DisplayName("Duration VO 테스트")
//class DurationTest {
//
//    @Test
//    @DisplayName("Duration 생성 성공")
//    void create_Success() {
//        // given
//        Integer value = 30;
//
//        // when
//        Duration duration = Duration.of(value);
//
//        // then
//        assertThat(duration.getValue()).isEqualTo(value);
//    }
//
//    @Test
//    @DisplayName("Duration 생성 실패 - 음수")
//    void create_Fail_Negative() {
//        // given
//        Integer value = -10;
//
//        // when & then
//        assertThatThrownBy(() -> Duration.of(value))
//                .isInstanceOf(IllegalArgumentException.class)
//                .hasMessage("시간은 음수일 수 없습니다");
//    }
//
//    @Test
//    @DisplayName("Duration 포맷팅 - 시간 포함")
//    void format_WithHours() {
//        // given
//        Duration duration = Duration.of(90);
//
//        // when
//        String formatted = duration.format();
//
//        // then
//        assertThat(formatted).isEqualTo("1시간 30분");
//    }
//
//    @Test
//    @DisplayName("Duration 포맷팅 - 분만")
//    void format_MinutesOnly() {
//        // given
//        Duration duration = Duration.of(45);
//
//        // when
//        String formatted = duration.format();
//
//        // then
//        assertThat(formatted).isEqualTo("45분");
//    }
//}
