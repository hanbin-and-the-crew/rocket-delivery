package org.sparta.order.infrastructure.repository;

import lombok.RequiredArgsConstructor;
import org.sparta.order.domain.entity.Stock;
import org.sparta.order.domain.repository.StockRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class StockRepositoryImpl implements StockRepository {

    private final StockJpaRepository stockJpaRepository;

    @Override
    public Optional<Stock> findById(UUID productId) {
        return stockJpaRepository.findById(productId);
    }

    @Override
    public Stock save(Stock stock) {
        return stockJpaRepository.save(stock);
    }
}
