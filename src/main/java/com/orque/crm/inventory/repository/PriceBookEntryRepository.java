package com.orque.crm.inventory.repository;

import com.orque.crm.inventory.entity.PriceBookEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PriceBookEntryRepository extends JpaRepository<PriceBookEntry, Long> {
    List<PriceBookEntry> findByPriceBookId(Long priceBookId);
    Optional<PriceBookEntry> findByPriceBookIdAndProductId(Long priceBookId, Long productId);
}
