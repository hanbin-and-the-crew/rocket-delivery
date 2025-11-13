package org.sparta.slack.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sparta.slack.application.service.notification.SlackEmailNotificationService;
import org.sparta.slack.presentation.dto.message.SlackEmailMessageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SlackDirectMessageController.class)
@AutoConfigureMockMvc(addFilters = false)
class SlackDirectMessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SlackEmailNotificationService slackEmailNotificationService;

    @Test
    @DisplayName("Slack DM 발송 요청이 성공하면 Slack 사용자 ID와 타임스탬프를 반환한다")
    void sendDirectMessage_ShouldReturnSlackMetadata() throws Exception {
        given(slackEmailNotificationService.sendDirectMessage(anyString(), anyString()))
                .willReturn(new SlackEmailNotificationService.SlackEmailMessageResult(
                        "ok",
                        "U12345678",
                        "1731465300.123"
                ));

        SlackEmailMessageRequest request = new SlackEmailMessageRequest(
                "hub.manager@rocket.delivery",
                "주문번호 1번, 12월 10일 오전 9시까지 발송하세요!"
        );

        mockMvc.perform(post("/api/slack/dm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slackUserId").value("U12345678"))
                .andExpect(jsonPath("$.data.slackTimestamp").value("1731465300.123"));
    }

    @Test
    @DisplayName("이메일이 비어 있으면 400 Bad Request를 응답한다")
    void sendDirectMessage_ShouldValidateEmail() throws Exception {
        String invalidPayload = """
                {
                  "email": "",
                  "message": "내용"
                }
                """;

        mockMvc.perform(post("/api/slack/dm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest());
    }
}
