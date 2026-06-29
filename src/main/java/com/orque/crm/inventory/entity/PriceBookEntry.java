package com.orque.crm.inventory.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "crm_price_book_entries")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceBookEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long priceBookId;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private BigDecimal customPrice;
}
