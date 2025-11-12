package org.sparta.slack.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.slack.domain.entity.Template;
import org.sparta.slack.domain.enums.Channel;
import org.sparta.slack.domain.enums.TemplateFormat;
import org.sparta.slack.infrastructure.repository.TemplateJpaRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Ensures the ROUTE_DAILY_SUMMARY template always exposes detailed delivery metrics.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RouteSummaryTemplateInitializer implements ApplicationRunner {

    static final String TEMPLATE_CODE = "ROUTE_DAILY_SUMMARY";
    static final String LEGACY_TEMPLATE_SIGNATURE = "담당자 {{managerName}} / 경로 {{routeSummary}}";
    static final String DEFAULT_TEMPLATE_CONTENT = """
            담당자 {{managerName}}님, {{dispatchDate}} 배송 계획입니다.

            • 허브: {{hubName}}
            • 경로: {{orderedStops}}
            • 예상 이동 거리: {{totalDistanceKm}}km
            • 예상 소요 시간: {{totalDurationMinutes}}분
            • AI 요약: {{routeSummary}}
            • 추천 사유: {{aiReason}}
            """;

    private final TemplateJpaRepository templateJpaRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Optional<Template> templateOptional = templateJpaRepository.findById(TEMPLATE_CODE);
        if (templateOptional.isPresent()) {
            updateIfLegacy(templateOptional.get());
        } else {
            createDefaultTemplate();
        }
    }

    private void updateIfLegacy(Template template) {
        if (isLegacyTemplate(template.getContent())) {
            template.updateContent(DEFAULT_TEMPLATE_CONTENT);
            templateJpaRepository.save(template);
            log.info("ROUTE_DAILY_SUMMARY 템플릿을 최신 형식으로 업데이트했습니다.");
        }
    }

    private boolean isLegacyTemplate(String content) {
        if (content == null) {
            return true;
        }
        String normalized = content.replaceAll("\\s+", " ").trim();
        return normalized.equals(LEGACY_TEMPLATE_SIGNATURE);
    }

    private void createDefaultTemplate() {
        Template template = Template.create(
                TEMPLATE_CODE,
                TemplateFormat.MARKDOWN,
                DEFAULT_TEMPLATE_CONTENT,
                Channel.SLACK,
                "일일 경로 요약"
        );
        templateJpaRepository.save(template);
        log.info("ROUTE_DAILY_SUMMARY 템플릿이 존재하지 않아 기본 템플릿을 생성했습니다.");
    }
}
