package org.sparta.slack.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.sparta.slack.application.port.out.SlackUserLookupPort;
import org.sparta.slack.infrastructure.adapter.SlackUserDirectoryAdapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SlackUserDirectoryAdapter 캐시 동작을 검증하는 테스트.
 */
class SlackUserDirectoryServiceTest {

    private SlackUserLookupPort slackUserLookupPort;
    private SlackUserDirectoryAdapter slackUserDirectoryAdapter;

    @BeforeEach
    void setUp() {
        slackUserLookupPort = Mockito.mock(SlackUserLookupPort.class);
        slackUserDirectoryAdapter = new SlackUserDirectoryAdapter(slackUserLookupPort);
    }

    @Test
    @DisplayName("이메일 기반 Slack 사용자 ID 조회 시 캐시를 활용한다")
    void resolveUserId_ShouldUseCache() {
        String email = "cache@test.com";
        when(slackUserLookupPort.lookupUserByEmail(email))
                .thenReturn(new SlackUserLookupPort.SlackUser("U123", email, "캐시 사용자"));

        String first = slackUserDirectoryAdapter.resolveUserId(email);
        String second = slackUserDirectoryAdapter.resolveUserId(email);

        assertThat(first).isEqualTo("U123");
        assertThat(second).isEqualTo("U123");
        verify(slackUserLookupPort, times(1)).lookupUserByEmail(email);
    }
}
