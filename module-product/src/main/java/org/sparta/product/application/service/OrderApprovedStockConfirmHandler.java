package org.sparta.product.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.product.domain.entity.ProcessedEvent;
import org.sparta.product.domain.repository.ProcessedEventRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderApprovedStockConfirmHandler {

    private final StockService stockService;
    private final ProcessedEventRepository processedEventRepository;

    @Transactional
    public void handle(UUID eventId, UUID orderId) {
        try {
            processedEventRepository.save(
                    ProcessedEvent.of(eventId, "OrderApprovedEvent")
            );
        } catch (DataIntegrityViolationException e) {
            log.debug("Duplicate event ignored: eventId={}", eventId);
            return;
        }

        stockService.confirmReservation(orderId.toString());
    }
}
