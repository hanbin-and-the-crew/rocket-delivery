package org.sparta.slack.application.port.out;

import org.sparta.slack.domain.entity.Template;

import java.util.Optional;

/**
 * Slack 알림용 템플릿을 조회하는 포트
 */
public interface SlackTemplateRepository {

    Optional<Template> findActiveByCode(String templateCode);
}
