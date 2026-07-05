package com.orque.crm.feature.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.math.BigDecimal;

/**
 * One product line on a Quote or Invoice. productName/unitPrice are a snapshot taken at
 * the time the line was added — editing the unit price here (an explicit user override)
 * never writes back to the actual Product record, and later renaming/repricing a Product
 * in Inventory doesn't retroactively change quotes/invoices that already reference it.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LineItem {
    private Long productId;
    private String productName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;
}
