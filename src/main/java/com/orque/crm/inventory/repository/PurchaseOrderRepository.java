package com.orque.crm.inventory.repository;

import com.orque.crm.inventory.entity.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    java.util.List<PurchaseOrder> findByOrganizationId(String organizationId);
}
