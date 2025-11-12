package org.sparta.slack.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** ORDER_DEADLINE_ALERT 템플릿 초기화 동작을 검증한다. */
@ExtendWith(MockitoExtension.class)
class OrderDeadlineTemplateInitializerTest {

    @Mock
    private TemplateJpaRepository templateJpaRepository;

    private OrderDeadlineTemplateInitializer initializer;

    @BeforeEach
    void setUp() {
        initializer = new OrderDeadlineTemplateInitializer(templateJpaRepository);
    }

    @Test
    @DisplayName("ORDER_DEADLINE_ALERT 템플릿이 없으면 새로 생성한다")
    void run_WhenTemplateMissing_CreatesTemplate() throws Exception {
        when(templateJpaRepository.findById(OrderDeadlineTemplateInitializer.TEMPLATE_CODE))
                .thenReturn(Optional.empty());

        initializer.run(new DefaultApplicationArguments(new String[0]));

        ArgumentCaptor<Template> captor = ArgumentCaptor.forClass(Template.class);
        verify(templateJpaRepository).save(captor.capture());
        Template saved = captor.getValue();
        assertThat(saved.getTemplateCode()).isEqualTo(OrderDeadlineTemplateInitializer.TEMPLATE_CODE);
        assertThat(saved.getContent()).isEqualTo(OrderDeadlineTemplateInitializer.DEFAULT_TEMPLATE_CONTENT);
        assertThat(saved.getChannel()).isEqualTo(Channel.SLACK);
        assertThat(saved.getFormat()).isEqualTo(TemplateFormat.MARKDOWN);
    }

    @Test
    @DisplayName("ORDER_DEADLINE_ALERT 템플릿이 기존 내용과 다르면 최신 내용으로 갱신한다")
    void run_WhenTemplateExistsButOutdated_UpdatesContent() throws Exception {
        Template template = Template.create(
                OrderDeadlineTemplateInitializer.TEMPLATE_CODE,
                TemplateFormat.MARKDOWN,
                "old",
                Channel.SLACK,
                "desc"
        );
        when(templateJpaRepository.findById(OrderDeadlineTemplateInitializer.TEMPLATE_CODE))
                .thenReturn(Optional.of(template));

        initializer.run(new DefaultApplicationArguments(new String[0]));

        assertThat(template.getContent()).isEqualTo(OrderDeadlineTemplateInitializer.DEFAULT_TEMPLATE_CONTENT);
        verify(templateJpaRepository).save(template);
    }
}
