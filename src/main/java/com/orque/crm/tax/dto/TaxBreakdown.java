package com.orque.crm.tax.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxBreakdown {
    private BigDecimal subtotal;
    private List<TaxComponent> taxes;
    private BigDecimal totalTax;
    private BigDecimal grandTotal;
    /** e.g. "GST", "VAT" — stored alongside the breakdown so old records keep their label. */
    private String taxSystem;
}
