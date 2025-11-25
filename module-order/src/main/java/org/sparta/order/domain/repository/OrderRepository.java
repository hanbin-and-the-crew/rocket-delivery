package org.sparta.order.domain.repository;

import org.sparta.order.domain.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository {

    /**
 * Persists the provided Order and returns the saved instance.
 *
 * @param order the Order to persist
 * @return the persisted Order, potentially including generated identifiers or updated audit fields
 */
Order save(Order order);

    /**
 * Finds an Order by its id when the Order has not been deleted (deletedAt is null).
 *
 * @param id the UUID of the Order
 * @return an Optional containing the Order if found and not deleted, or an empty Optional otherwise
 */
Optional<Order> findByIdAndDeletedAtIsNull(UUID id);

    /**
 * Retrieves a page of orders for the specified customer that are not deleted.
 *
 * @param customerId the UUID of the customer whose orders to retrieve
 * @param pageable   pagination and sorting information
 * @return a page of orders for the specified customer where `deletedAt` is null
 */
Page<Order> findByCustomerIdAndDeletedAtIsNull(UUID customerId, Pageable pageable);
}