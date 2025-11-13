package org.sparta.slack.domain.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sparta.slack.domain.enums.Channel;
import org.sparta.slack.domain.enums.TemplateFormat;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Template 렌더링과 상태 변경이 올바른지 검증한다. */
class TemplateTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    @DisplayName("페이로드 값으로 템플릿 플레이스홀더를 치환한다")
    void render_ShouldReplaceSimplePlaceholders() throws Exception {
        Template template = Template.create(
                "ORDER_DEADLINE_NOTICE",
                TemplateFormat.MARKDOWN,
                "주문번호 {{orderNumber}} / 고객 {{customerName}} / 마감 {{finalDeadline}} / 비고 {{missingField}}",
                Channel.SLACK,
                "주문 납기 안내"
        );

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderNumber", "ORD-001");
        payload.put("customerName", "홍길동");
        payload.put("finalDeadline", LocalDateTime.of(2025, 12, 10, 9, 0));

        String payloadJson = objectMapper.writeValueAsString(payload);

        String rendered = template.render(payloadJson);

        assertThat(rendered).isEqualTo("주문번호 ORD-001 / 고객 홍길동 / 마감 2025-12-10T09:00:00 / 비고 ");
    }

    @Test
    @DisplayName("중첩 객체나 배열 키도 평탄화하여 치환할 수 있다")
    void render_ShouldSupportNestedKeys() throws Exception {
        Template template = Template.create(
                "ORDER_ROUTE_NOTICE",
                TemplateFormat.PLAIN_TEXT,
                "담당자 {{deliveryManager.name}}({{deliveryManager.email}}) / 첫 경유지 {{transitPath[0]}}",
                Channel.SLACK,
                "경로 정보"
        );

        Map<String, Object> manager = Map.of(
                "name", "고길동",
                "email", "kdk@sparta.world"
        );
        Map<String, Object> payload = Map.of(
                "deliveryManager", manager,
                "transitPath", List.of("경기 북부 센터", "대전광역시 센터")
        );

        String payloadJson = objectMapper.writeValueAsString(payload);

        String rendered = template.render(payloadJson);

        assertThat(rendered).isEqualTo("담당자 고길동(kdk@sparta.world) / 첫 경유지 경기 북부 센터");
    }
}
