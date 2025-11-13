package org.sparta.slack.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sparta.slack.domain.entity.Template;
import org.sparta.slack.domain.enums.Channel;
import org.sparta.slack.domain.enums.TemplateFormat;
import org.sparta.slack.infrastructure.repository.TemplateJpaRepository;
import org.springframework.boot.DefaultApplicationArguments;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/** RouteSummaryTemplateInitializer의 생성/업데이트 동작을 검증한다. */
@ExtendWith(MockitoExtension.class)
class RouteSummaryTemplateInitializerTest {

    @Mock
    private TemplateJpaRepository templateJpaRepository;

    private RouteSummaryTemplateInitializer initializer;

    @BeforeEach
    void setUp() {
        initializer = new RouteSummaryTemplateInitializer(templateJpaRepository);
    }

    @Test
    void updatesLegacyTemplateContent() throws Exception {
        Template legacy = Template.create(
                RouteSummaryTemplateInitializer.TEMPLATE_CODE,
                TemplateFormat.MARKDOWN,
                RouteSummaryTemplateInitializer.LEGACY_TEMPLATE_SIGNATURE,
                Channel.SLACK,
                "legacy"
        );

        when(templateJpaRepository.findById(RouteSummaryTemplateInitializer.TEMPLATE_CODE))
                .thenReturn(Optional.of(legacy));

        initializer.run(new DefaultApplicationArguments(new String[0]));

        assertThat(legacy.getContent()).isEqualTo(RouteSummaryTemplateInitializer.DEFAULT_TEMPLATE_CONTENT);
        verify(templateJpaRepository).save(legacy);
    }

    @Test
    void createsTemplateWhenMissing() throws Exception {
        when(templateJpaRepository.findById(RouteSummaryTemplateInitializer.TEMPLATE_CODE))
                .thenReturn(Optional.empty());

        initializer.run(new DefaultApplicationArguments(new String[0]));

        ArgumentCaptor<Template> captor = ArgumentCaptor.forClass(Template.class);
        verify(templateJpaRepository).save(captor.capture());
        Template saved = captor.getValue();

        assertThat(saved.getTemplateCode()).isEqualTo(RouteSummaryTemplateInitializer.TEMPLATE_CODE);
        assertThat(saved.getContent()).isEqualTo(RouteSummaryTemplateInitializer.DEFAULT_TEMPLATE_CONTENT);
        assertThat(saved.getChannel()).isEqualTo(Channel.SLACK);
    }
}
