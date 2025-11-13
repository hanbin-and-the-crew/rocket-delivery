package org.sparta.slack.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sparta.slack.application.service.message.SlackMessageCommandService;
import org.sparta.slack.application.service.message.SlackMessageQueryService;
import org.sparta.slack.domain.enums.MessageStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SlackMessageAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
class SlackMessageAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SlackMessageCommandService commandService;

    @MockBean
    private SlackMessageQueryService queryService;

    @Test
    @DisplayName("Slack 메시지 생성 API는 템플릿 코드와 Slack ID를 응답한다")
    void createMessage_ShouldReturnDetail() throws Exception {
        UUID messageId = UUID.randomUUID();
        SlackMessageResponse.Detail detail = new SlackMessageResponse.Detail(
                messageId,
                "ORDER_DEADLINE_ALERT",
                "U12345678",
                MessageStatus.SENT,
                LocalDateTime.of(2025, 12, 10, 9, 0),
                "본문",
                "1731465300.123"
        );
        given(commandService.create(any(SlackMessageRequest.Create.class))).willReturn(detail);

        SlackMessageRequest.Create request = new SlackMessageRequest.Create(
                "U12345678",
                "ORDER_DEADLINE_ALERT",
                Map.of("orderId", "ORDER-1")
        );

        mockMvc.perform(post("/api/slack/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.templateCode").value("ORDER_DEADLINE_ALERT"))
                .andExpect(jsonPath("$.data.slackId").value("U12345678"));
    }

    @Test
    @DisplayName("Slack 메시지 검색 API는 PageResponse 형태로 결과를 반환한다")
    void searchMessages_ShouldReturnPagedContent() throws Exception {
        SlackMessageResponse.Summary summary = new SlackMessageResponse.Summary(
                UUID.randomUUID(),
                "ROUTE_DAILY_SUMMARY",
                "U999",
                MessageStatus.SENT,
                LocalDateTime.of(2025, 12, 11, 6, 0)
        );
        PageResponse<SlackMessageResponse.Summary> pageResponse = new PageResponse<>(
                List.of(summary),
                0,
                20,
                1,
                1
        );
        given(queryService.search(any(SlackMessageSearchRequest.Query.class), any(Pageable.class)))
                .willReturn(pageResponse);

        mockMvc.perform(get("/api/slack/messages")
                        .param("templateCode", "ROUTE_DAILY_SUMMARY")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].templateCode").value("ROUTE_DAILY_SUMMARY"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }
}
