package org.sparta.slack.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.sparta.jpa.entity.BaseEntity;
import org.sparta.slack.domain.enums.Channel;
import org.sparta.slack.domain.enums.TemplateFormat;

/**
 * Template Aggregate Root
 * 메시지 템플릿 관리
 * DB Seed로 초기 데이터 관리
 */
@Entity
@Getter
@Table(name = "p_templates")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Template extends BaseEntity {

    @Id
    @Column(name = "template_code", length = 100)
    private String templateCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TemplateFormat format;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Channel channel;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    private Template(
            String templateCode,
            TemplateFormat format,
            String content,
            Channel channel,
            String description
    ) {
        this.templateCode = templateCode;
        this.format = format;
        this.content = content;
        this.channel = channel;
        this.description = description;
        this.isActive = true;
    }

    /**
     * 템플릿 생성 팩토리 메서드
     */
    public static Template create(
            String templateCode,
            TemplateFormat format,
            String content,
            Channel channel,
            String description
    ) {
        validateTemplateCode(templateCode);
        validateFormat(format);
        validateContent(content);
        validateChannel(channel);

        return new Template(templateCode, format, content, channel, description);
    }

    private static void validateTemplateCode(String templateCode) {
        if (templateCode == null || templateCode.isBlank()) {
            throw new IllegalArgumentException("템플릿 코드는 필수입니다");
        }
        if (templateCode.length() > 100) {
            throw new IllegalArgumentException("템플릿 코드는 100자를 초과할 수 없습니다");
        }
    }

    private static void validateFormat(TemplateFormat format) {
        if (format == null) {
            throw new IllegalArgumentException("템플릿 포맷은 필수입니다");
        }
    }

    private static void validateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("템플릿 내용은 필수입니다");
        }
    }

    private static void validateChannel(Channel channel) {
        if (channel == null) {
            throw new IllegalArgumentException("채널은 필수입니다");
        }
    }

    /**
     * 템플릿 내용 수정
     */
    public void updateContent(String content) {
        validateContent(content);
        this.content = content;
    }

    /**
     * 템플릿 포맷 수정
     */
    public void updateFormat(TemplateFormat format) {
        validateFormat(format);
        this.format = format;
    }

    /**
     * 템플릿 설명 수정
     */
    public void updateDescription(String description) {
        this.description = description;
    }

    /**
     * 템플릿 비활성화
     */
    public void deactivate() {
        this.isActive = false;
    }

    /**
     * 템플릿 활성화
     */
    public void activate() {
        this.isActive = true;
    }

    /**
     * 변수를 실제 값으로 치환하여 메시지 렌더링
     * 예: "안녕하세요 {{name}}님" + {"name": "홍길동"} -> "안녕하세요 홍길동님"
     */
    public String render(String payloadJson) {
        if (payloadJson == null || payloadJson.isBlank()) {
            return this.content;
        }

        // TODO: 실제 구현시 JSON 파싱 및 템플릿 엔진 사용
        // 현재는 단순 반환
        return this.content;
    }
}