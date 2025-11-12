package org.sparta.slack.infrastructure.repository;

import java.util.Optional;

import org.sparta.slack.domain.entity.Template;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Template JPA 저장소
 */
@Repository
public interface TemplateJpaRepository extends JpaRepository<Template, String> {

    Optional<Template> findByTemplateCodeAndIsActiveTrue(String templateCode);
}
