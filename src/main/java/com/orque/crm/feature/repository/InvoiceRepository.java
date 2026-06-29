package com.orque.crm.feature.repository;

import com.orque.crm.feature.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByQuoteId(Long quoteId);
    List<Invoice> findByDealId(Long dealId);
    List<Invoice> findByContactIgnoreCase(String contact);
    List<Invoice> findByAccountIgnoreCase(String account);
}
