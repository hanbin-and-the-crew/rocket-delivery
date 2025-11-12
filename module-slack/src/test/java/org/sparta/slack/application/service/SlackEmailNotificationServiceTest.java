package org.sparta.slack.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.sparta.slack.application.port.out.SlackDmPort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SlackEmailNotificationServiceTest {

    private SlackUserDirectoryService slackUserDirectoryService;
    private SlackDmPort slackDmPort;
    private SlackEmailNotificationService slackEmailNotificationService;

    @BeforeEach
    void setUp() {
        slackUserDirectoryService = Mockito.mock(SlackUserDirectoryService.class);
        slackDmPort = Mockito.mock(SlackDmPort.class);
        slackEmailNotificationService = new SlackEmailNotificationService(slackUserDirectoryService, slackDmPort);
    }

    @Test
    @DisplayName("이메일로 Slack DM을 전송한다")
    void sendDirectMessage_ShouldLookupAndSend() {
        String email = "test@example.com";
        String userId = "U999";
        when(slackUserDirectoryService.resolveUserId(email)).thenReturn(userId);
        when(slackDmPort.sendMessage(userId, "hello")).thenReturn(new SlackDmPort.SlackDmResult("12345.6789"));

        SlackEmailNotificationService.SlackEmailMessageResult result =
                slackEmailNotificationService.sendDirectMessage(email, "hello");

        assertThat(result.result()).isEqualTo("ok");
        assertThat(result.slackUserId()).isEqualTo(userId);
        assertThat(result.slackTimestamp()).isEqualTo("12345.6789");

        verify(slackUserDirectoryService).resolveUserId(email);
        verify(slackDmPort).sendMessage(userId, "hello");
    }
}
