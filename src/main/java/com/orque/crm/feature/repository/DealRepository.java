package com.orque.crm.feature.repository;

import com.orque.crm.feature.entity.Deal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DealRepository extends JpaRepository<Deal, Long> {
    List<Deal> findByContactIgnoreCase(String contact);
    List<Deal> findByAccountIgnoreCase(String account);
    List<Deal> findByAssignedTo(String assignedTo);
}
