package com.orque.crm.inventory.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "crm_warehouses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Warehouse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String location;

    private String organizationId;
}
