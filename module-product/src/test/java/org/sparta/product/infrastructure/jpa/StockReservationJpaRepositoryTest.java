package org.sparta.product.infrastructure.jpa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sparta.product.domain.entity.StockReservation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class StockReservationJpaRepositoryTest {

    @Autowired
    private StockReservationJpaRepository stockReservationJpaRepository;

    @Test
    @DisplayName("StockReservationJpaRepository.findByReservationKey: internalReservationKey(reservation_key)로 단건 조회")
    void findByReservationKey_works() {
        UUID stockId = UUID.randomUUID();
        String externalKey = "order-1";
        String internalKey = "order-1:" + UUID.randomUUID();

        StockReservation reservation = StockReservation.reserve(stockId, externalKey, internalKey, 3);
        StockReservation saved = stockReservationJpaRepository.save(reservation);
        assertNotNull(saved.getId());

        Optional<StockReservation> found = stockReservationJpaRepository.findByReservationKey(internalKey);
        assertTrue(found.isPresent());
        assertEquals(externalKey, found.get().getExternalReservationKey());
        assertEquals(internalKey, found.get().getReservationKey());
        assertEquals(3, found.get().getReservedQuantity());

        assertTrue(stockReservationJpaRepository.findByReservationKey("missing").isEmpty());
    }

    @Test
    @DisplayName("StockReservationJpaRepository.findAllByExternalReservationKey: externalReservationKey로 주문 다품목 조회")
    void findAllByExternalReservationKey_works() {
        String externalKey = "order-2";
        UUID stockId1 = UUID.randomUUID();
        UUID stockId2 = UUID.randomUUID();

        StockReservation r1 = StockReservation.reserve(stockId1, externalKey, externalKey + ":" + UUID.randomUUID(), 2);
        StockReservation r2 = StockReservation.reserve(stockId2, externalKey, externalKey + ":" + UUID.randomUUID(), 5);
        StockReservation other = StockReservation.reserve(UUID.randomUUID(), "order-x", "order-x:" + UUID.randomUUID(), 1);

        stockReservationJpaRepository.saveAll(List.of(r1, r2, other));

        List<StockReservation> found = stockReservationJpaRepository.findAllByExternalReservationKey(externalKey);
        assertEquals(2, found.size());
        assertTrue(found.stream().allMatch(r -> externalKey.equals(r.getExternalReservationKey())));

        assertTrue(stockReservationJpaRepository.findAllByExternalReservationKey("missing").isEmpty());
    }
}
