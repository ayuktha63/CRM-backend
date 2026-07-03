package com.orque.crm.inventory.repository;

import com.orque.crm.inventory.entity.PriceBook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PriceBookRepository extends JpaRepository<PriceBook, Long> {
    java.util.List<PriceBook> findByOrganizationId(String organizationId);
}
