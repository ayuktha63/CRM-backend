package com.orque.crm.feature.repository;

import com.orque.crm.feature.entity.Quote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuoteRepository extends JpaRepository<Quote, Long> {
    List<Quote> findByDealId(Long dealId);
    List<Quote> findByContactIgnoreCase(String contact);
    List<Quote> findByAccountIgnoreCase(String account);

    java.util.List<com.orque.crm.feature.entity.Quote> findByOrganizationId(String organizationId);
    java.util.List<com.orque.crm.feature.entity.Quote> findByOrganizationIdAndCreatedBy(String organizationId, String createdBy);
}
