package org.sparta.slack.config;

import lombok.RequiredArgsConstructor;
import org.sparta.slack.domain.entity.Template;
import org.sparta.slack.domain.enums.Channel;
import org.sparta.slack.domain.enums.TemplateFormat;
import org.sparta.slack.infrastructure.repository.TemplateJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * ORDER_DEADLINE_ALERT 템플릿 시딩/업데이트 담당.
 */
@Component
@RequiredArgsConstructor
public class OrderDeadlineTemplateInitializer implements ApplicationRunner {

    static final String TEMPLATE_CODE = "ORDER_DEADLINE_ALERT";

    static final String DEFAULT_TEMPLATE_CONTENT = """
             주문 {{orderNumber}} 발송 시한 안내

            • 고객: {{customerName}} ({{customerEmail}})
            • 상품 정보: {{productInfo}}
            • 요청 메모: {{requestMemo}}
            • 경로 요약: {{routeSummary}}
            • 최종 발송 시한: {{finalDeadline}}
            • AI 사유: {{aiReason}}

            담당자: {{deliveryManagerName}} {{deliveryManagerEmail}}
            """;

    private final TemplateJpaRepository templateJpaRepository;
    private static final Logger log = LoggerFactory.getLogger(OrderDeadlineTemplateInitializer.class);

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        templateJpaRepository.findById(TEMPLATE_CODE)
                .ifPresentOrElse(this::refreshIfNeeded, this::createDefault);
    }

    private void refreshIfNeeded(Template template) {
        if (!DEFAULT_TEMPLATE_CONTENT.equals(template.getContent())) {
            template.updateContent(DEFAULT_TEMPLATE_CONTENT);
            templateJpaRepository.save(template);
            log.info("ORDER_DEADLINE_ALERT 템플릿 내용을 최신화했습니다.");
        }
    }

    private void createDefault() {
        Template template = Template.create(
                TEMPLATE_CODE,
                TemplateFormat.MARKDOWN,
                DEFAULT_TEMPLATE_CONTENT,
                Channel.SLACK,
                "주문 발송 시한 안내"
        );
        templateJpaRepository.save(template);
        log.info("ORDER_DEADLINE_ALERT 기본 템플릿을 생성했습니다.");
    }
}
