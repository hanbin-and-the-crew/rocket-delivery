package org.sparta.order.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.order.domain.entity.IdempotencyRecord;
import org.sparta.order.domain.repository.IdempotencyRepository;
import org.sparta.order.presentation.dto.response.OrderResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final IdempotencyRepository idempotencyRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public Optional<OrderResponse.Detail> findExistingResponse(String idempotencyKey) {
        return idempotencyRepository.findByIdempotencyKey(idempotencyKey)
                .filter(record -> !record.isExpired())
                .map(record -> {
                    try {
                        return objectMapper.readValue(record.getResponseBody(), OrderResponse.Detail.class);
                    } catch (JsonProcessingException e) {
                        log.error("Failed to deserialize idempotency response", e);
                        return null;
                    }
                });
    }

    @Transactional
    public void saveIdempotencyRecord(String idempotencyKey, String orderId,
                                      OrderResponse.Detail response, int statusCode) {
        try {
            String responseBody = objectMapper.writeValueAsString(response);
            IdempotencyRecord record = IdempotencyRecord.create(
                    idempotencyKey, orderId, responseBody, statusCode);
            idempotencyRepository.save(record);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize idempotency response", e);
        }
    }

    @Transactional
    public boolean tryAcquireLock(String idempotencyKey) {
        // 이미 존재하면 false, 없으면 placeholder 저장 후 true
        if (idempotencyRepository.findByIdempotencyKey(idempotencyKey).isPresent()) {
            return false;
        }
        // Placeholder로 먼저 저장 (동시성 제어)
        IdempotencyRecord placeholder = IdempotencyRecord.create(
                idempotencyKey, "PROCESSING", null, 0);
        idempotencyRepository.save(placeholder);
        return true;
    }

    public void deleteRecord(String idempotencyKey) {
    }
}
