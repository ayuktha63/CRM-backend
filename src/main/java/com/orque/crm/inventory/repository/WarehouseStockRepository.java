package com.orque.crm.inventory.repository;

import com.orque.crm.inventory.entity.WarehouseStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseStockRepository extends JpaRepository<WarehouseStock, Long> {
    List<WarehouseStock> findByWarehouseId(Long warehouseId);
    Optional<WarehouseStock> findByWarehouseIdAndProductId(Long warehouseId, Long productId);
}
