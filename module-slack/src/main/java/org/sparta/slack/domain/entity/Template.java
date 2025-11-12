package org.sparta.slack.domain.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.sparta.jpa.entity.BaseEntity;
import org.sparta.slack.domain.enums.Channel;
import org.sparta.slack.domain.enums.TemplateFormat;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private static final ObjectMapper PAYLOAD_OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .findAndRegisterModules()
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{\\s*([^{}]+?)\\s*}}");
    private static final java.time.format.DateTimeFormatter ISO_SECOND_FORMATTER =
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

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

        Map<String, String> payloadValues = parsePayload(payloadJson);
        if (payloadValues.isEmpty()) {
            return this.content;
        }

        return applyPlaceholders(this.content, payloadValues);
    }

    private Map<String, String> parsePayload(String payloadJson) {
        try {
            JsonNode root = PAYLOAD_OBJECT_MAPPER.readTree(payloadJson);
            Map<String, String> flattened = new LinkedHashMap<>();
            flattenNode("", root, flattened);
            return flattened;
        } catch (JsonProcessingException ex) {
            return Map.of();
        }
    }

    private void flattenNode(String prefix, JsonNode node, Map<String, String> accumulator) {
        if (node == null || node.isNull()) {
            if (!prefix.isBlank()) {
                accumulator.put(prefix, "");
            }
            return;
        }

        if (node.isObject()) {
            node.fields().forEachRemaining(entry -> {
                String childPrefix = prefix.isBlank()
                        ? entry.getKey()
                        : prefix + "." + entry.getKey();
                flattenNode(childPrefix, entry.getValue(), accumulator);
            });
            return;
        }

        if (node.isArray()) {
            if (!prefix.isBlank()) {
                accumulator.put(prefix, scalarValue(node));
            }
            int index = 0;
            for (JsonNode child : node) {
                String childPrefix = prefix + "[" + index + "]";
                flattenNode(childPrefix, child, accumulator);
                index++;
            }
            return;
        }

        if (prefix.isBlank()) {
            return;
        }
        accumulator.put(prefix, node.asText());
    }

    private String applyPlaceholders(String templateBody, Map<String, String> values) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(templateBody);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String key = matcher.group(1).trim();
            String replacement = values.getOrDefault(key, "");
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private String scalarValue(JsonNode node) {
        if (node.isValueNode()) {
            return node.asText();
        }
        if (node.isArray() && node.size() >= 3) {
            try {
                int year = node.get(0).asInt();
                int month = node.get(1).asInt();
                int day = node.get(2).asInt();
                int hour = node.size() > 3 ? node.get(3).asInt() : 0;
                int minute = node.size() > 4 ? node.get(4).asInt() : 0;
                int second = node.size() > 5 ? node.get(5).asInt() : 0;
                java.time.LocalDateTime dateTime = java.time.LocalDateTime.of(year, month, day, hour, minute, second);
                return dateTime.format(ISO_SECOND_FORMATTER);
            } catch (Exception ignored) {
                // ignore and fall back to JSON text
            }
        }
        return node.toString();
    }
}
