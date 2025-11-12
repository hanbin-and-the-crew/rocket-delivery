package org.sparta.slack.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sparta.slack.application.command.OrderDeadlineCommand;
import org.sparta.slack.application.service.notification.OrderDeadlinePlanResult;
import org.sparta.slack.application.service.notification.OrderDeadlinePlanningService;
import org.sparta.slack.config.properties.AiPlanningProperties;
import org.sparta.slack.domain.enums.UserRole;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/** OrderDeadlinePlanningService의 AI/Fallback 로직을 검증한다. */
@ExtendWith(MockitoExtension.class)
class OrderDeadlinePlanningServiceTest {

    @Mock
    private RestClient.Builder restClientBuilder;
    @Mock
    private RestClient restClient;
    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;
    @Mock
    private RestClient.RequestBodySpec requestBodySpec;
    @Mock
    private RestClient.ResponseSpec responseSpec;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private OrderDeadlinePlanningService service;

    @BeforeEach
    void setUp() {
        AiPlanningProperties properties = new AiPlanningProperties(
                "test-key",
                "https://gemini.test",
                "gemini-test",
                null,
                null
        );
        service = new OrderDeadlinePlanningService(properties, restClientBuilder, objectMapper);

        lenient().when(restClientBuilder.baseUrl(anyString())).thenReturn(restClientBuilder);
        lenient().when(restClientBuilder.build()).thenReturn(restClient);
        lenient().when(restClient.post()).thenReturn(requestBodyUriSpec);
        lenient().when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.body(anyString())).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    @DisplayName("plan 메서드는 AI 응답이 유효하면 최종 시한을 반환한다")
    void plan_WithValidAiResponse_ReturnsAiDeadline() throws Exception {
        JsonNode response = objectMapper.readTree("""
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": "{\\"finalDeadline\\":\\"2024-08-01T10:00\\",\\"routeSummary\\":\\"서울 → 부산\\",\\"reason\\":\\"AI 이유\\"}"
                          }
                        ]
                      }
                    }
                  ]
                }
                """);
        when(responseSpec.body(ArgumentMatchers.<Class<JsonNode>>any())).thenReturn(response);

        OrderDeadlinePlanResult result = service.plan(sampleCommand());

        assertThat(result.finalDeadline()).isEqualTo(LocalDateTime.of(2024, 8, 1, 10, 0));
        assertThat(result.routeSummary()).isEqualTo("서울 → 부산");
        assertThat(result.reason()).isEqualTo("AI 이유");
        assertThat(result.fallbackUsed()).isFalse();
    }

    @Test
    @DisplayName("plan 메서드는 AI 설정이 비활성화되면 Fallback 결과를 사용한다")
    void plan_WhenAiDisabled_UsesFallbackResult() {
        OrderDeadlinePlanningService disabledService = new OrderDeadlinePlanningService(
                new AiPlanningProperties(null, null, null, null, null),
                restClientBuilder,
                objectMapper
        );

        OrderDeadlinePlanResult result = disabledService.plan(sampleCommand());

        assertThat(result.fallbackUsed()).isTrue();
        assertThat(result.finalDeadline()).isNotNull();
    }

    private OrderDeadlineCommand sampleCommand() {
        return new OrderDeadlineCommand(
                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                "ORD-123",
                "홍길동",
                "hong@test.com",
                LocalDateTime.of(2024, 7, 1, 9, 0),
                "테스트 상품",
                10,
                "긴급",
                "서울 허브",
                "대전 경유",
                "부산 항만",
                UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
                UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"),
                LocalDateTime.of(2024, 7, 3, 18, 0),
                9,
                18,
                Set.of(UserRole.HUB_MANAGER),
                "김담당",
                "manager@test.com"
        );
    }
}
