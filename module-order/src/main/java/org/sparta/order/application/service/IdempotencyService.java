package org.sparta.order.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.order.domain.entity.IdempotencyRecord;
import org.sparta.order.domain.repository.IdempotencyRepository;
import org.sparta.order.presentation.dto.response.OrderResponse;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final IdempotencyRepository idempotencyRepository;
    private final ObjectMapper objectMapper;

//    @Lazy
//    private final IdempotencyService self;

    @Transactional(readOnly = true)
    public Optional<OrderResponse.Detail> findExistingResponse(String idempotencyKey) {
        return idempotencyRepository.findByIdempotencyKey(idempotencyKey)
                .filter(record -> !record.isExpired())
                .flatMap(record -> {    // flatMap => Optional.empty()를 명시적으로 반환할 수 있다고 함
                    try {
                        OrderResponse.Detail response = objectMapper.readValue(
                                record.getResponseBody(),
                                OrderResponse.Detail.class
                        );
                        return Optional.of(response);
                    } catch (JsonProcessingException e) {
                        log.error("Failed to deserialize idempotency response for key: {}. Deleting corrupted record.",
                                idempotencyKey, e);
                        /** 이 경우 : Spring AOP는 프록시 기반으로 동작, findExistingResponse를 외부에서 호출해야 프록시를 거쳐 트랜잭션이 적용됨
                         * 근데, findExistingResponse 내부에서 deleteCorruptedRecord호출하면 프롲기 거치지X, 트랜잭션 무시됨
                         * => 읽기 전용 트랜잭션에서 삭제 시도하게 되서 에러 발생함
                         * */
                        // 손상된 레코드 삭제 (별도 트랜잭션에서 처리)
                        // 일단 이 부분을 다시 살려두고 알아봐야될듯 이렇게 해두면 에러는 안남
                        deleteCorruptedRecord(idempotencyKey);

                        // 수정 => self를 통해 프록시를 거쳐서 호출
                        // 근데 이 부분이 문제가 되는데..?
//                        self.deleteCorruptedRecord(idempotencyKey);
                        return Optional.empty();
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
            log.debug("Saved idempotency record: key={}, orderId={}", idempotencyKey, orderId);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize idempotency response for key: {}", idempotencyKey, e);
            throw new IllegalStateException("Failed to save idempotency record: "+ idempotencyKey, e);
        }
    }

    @Transactional
    public boolean tryAcquireLock(String idempotencyKey) {
        try {
            // Unique constraint를 활용한 원자적 삽입 시도
            IdempotencyRecord placeholder = IdempotencyRecord.create(
                    idempotencyKey, "PROCESSING", null, 0);
            idempotencyRepository.save(placeholder);
            idempotencyRepository.flush(); // 즉시 DB 반영하여 예외 발생 유도
            log.debug("Lock acquired for idempotency key: {}", idempotencyKey);
            return true;
        } catch (DataIntegrityViolationException e) {
            // 이미 존재하는 경우 (동시 요청에 의해 먼저 생성됨)
            log.debug("Lock acquisition failed - key already exists: {}", idempotencyKey);
            return false;
        } catch (Exception e) {
            // 예상치 못한 예외는 상위로 전파
            log.error("Unexpected error while acquiring lock for key: {}", idempotencyKey, e);
            throw new IllegalStateException("Failed to acquire idempotency lock", e);
        }
    }


    @Transactional
    public void deleteRecord(String idempotencyKey) {
        idempotencyRepository.findByIdempotencyKey(idempotencyKey)
                .ifPresent(idempotencyRepository::delete);
    }

    /**
     * 역직렬화 실패한 손상된 레코드 삭제
     * 새로운 트랜잭션에서 실행되도록 별도 메서드로 분리
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void deleteCorruptedRecord(String idempotencyKey) {
        idempotencyRepository.findByIdempotencyKey(idempotencyKey)
                .ifPresent(record -> {
                    log.warn("Deleting corrupted idempotency record: key={}, orderId={}",
                            idempotencyKey, record.getOrderId());
                    idempotencyRepository.delete(record);
                });
    }
}
