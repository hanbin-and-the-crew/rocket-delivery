package org.sparta.slack.domain.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;
import org.sparta.slack.domain.vo.RouteStopSnapshot;

import java.util.Collections;
import java.util.List;

/**
 * RouteStopSnapshot <-> JSON 문자열 변환기
 */
@Slf4j
@Converter(autoApply = false)
public class RouteStopConverter implements AttributeConverter<List<RouteStopSnapshot>, String> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());
    private static final TypeReference<List<RouteStopSnapshot>> TYPE = new TypeReference<>() {};

    @Override
    public String convertToDatabaseColumn(List<RouteStopSnapshot> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "[]";
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(attribute);
        } catch (Exception ex) {
            log.error("RouteStopSnapshot 직렬화 실패", ex);
            throw new IllegalStateException("경로 지점 정보를 직렬화할 수 없습니다", ex);
        }
    }

    @Override
    public List<RouteStopSnapshot> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return Collections.emptyList();
        }
        try {
            String sanitized = dbData
                    .replace("\r", "")
                    .replace("\n", "");
            return OBJECT_MAPPER.readValue(sanitized, TYPE);
        } catch (Exception ex) {
            log.error("RouteStopSnapshot 역직렬화 실패 - {}", dbData, ex);
            return Collections.emptyList();
        }
    }
}
