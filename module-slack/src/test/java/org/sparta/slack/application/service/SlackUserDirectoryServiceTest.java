package org.sparta.slack.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.sparta.slack.application.port.out.SlackUserLookupPort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SlackUserDirectoryServiceTest {

    private SlackUserLookupPort slackUserLookupPort;
    private SlackUserDirectoryService slackUserDirectoryService;

    @BeforeEach
    void setUp() {
        slackUserLookupPort = Mockito.mock(SlackUserLookupPort.class);
        slackUserDirectoryService = new SlackUserDirectoryService(slackUserLookupPort);
    }

    @Test
    @DisplayName("이메일 기반 Slack 사용자 ID 조회 시 캐시를 활용한다")
    void resolveUserId_ShouldUseCache() {
        String email = "cache@test.com";
        when(slackUserLookupPort.lookupUserByEmail(email))
                .thenReturn(new SlackUserLookupPort.SlackUser("U123", email, "캐시 사용자"));

        String first = slackUserDirectoryService.resolveUserId(email);
        String second = slackUserDirectoryService.resolveUserId(email);

        assertThat(first).isEqualTo("U123");
        assertThat(second).isEqualTo("U123");
        verify(slackUserLookupPort, times(1)).lookupUserByEmail(email);
    }
}
