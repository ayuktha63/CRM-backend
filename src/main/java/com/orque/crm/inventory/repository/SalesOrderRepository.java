package com.orque.crm.inventory.repository;

import com.orque.crm.inventory.entity.SalesOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long> {
    java.util.List<SalesOrder> findByOrganizationId(String organizationId);
}
