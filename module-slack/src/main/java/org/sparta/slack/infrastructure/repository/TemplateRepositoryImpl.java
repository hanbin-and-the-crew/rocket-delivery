package org.sparta.slack.infrastructure.repository;

import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.sparta.slack.application.port.out.SlackTemplateRepository;
import org.sparta.slack.domain.entity.Template;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class TemplateRepositoryImpl implements SlackTemplateRepository {

    private final TemplateJpaRepository templateJpaRepository;

    @Override
    public Optional<Template> findActiveByCode(String templateCode) {
        return templateJpaRepository.findByTemplateCodeAndIsActiveTrue(templateCode);
    }
}
