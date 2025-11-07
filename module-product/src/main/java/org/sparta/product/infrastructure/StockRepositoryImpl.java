package org.sparta.product.infrastructure;

import lombok.RequiredArgsConstructor;
import org.sparta.product.domain.entity.Stock;
import org.sparta.product.domain.repository.StockRepository;
import org.sparta.product.infrastructure.jpa.StockJpaRepository;
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
}
