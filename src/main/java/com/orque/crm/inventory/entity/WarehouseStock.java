package com.orque.crm.inventory.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "crm_warehouse_stock")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarehouseStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long warehouseId;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Integer quantity;
}
