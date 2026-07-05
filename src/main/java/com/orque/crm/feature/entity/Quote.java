package com.orque.crm.feature.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quotes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Quote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /** Organization this record belongs to. Populated automatically by the backend. */
    @Column(length = 36)
    private String organizationId;


    @Column(nullable = false)
    private String quoteNumber;

    @Column(nullable = false)
    private String contact;

    private String account;
    private BigDecimal amount;
    private LocalDate validUntil;
    private String createdBy;
    private String status;
    private Long dealId;

    /** Product lines. When present, `amount` is always recomputed as their sum. */
    @ElementCollection
    @CollectionTable(name = "quote_line_items", joinColumns = @JoinColumn(name = "quote_id"))
    @Builder.Default
    private List<LineItem> lineItems = new ArrayList<>();

    /** Free-text state used only to pick GST same-state (CGST/SGST) vs different-state (IGST). */
    private String customerState;

    /**
     * Tax breakdown snapshot computed by TaxCalculationService at save time — never
     * recomputed later, so changing the org's country/tax config afterward doesn't
     * retroactively change quotes/invoices that already exist.
     */
    private String taxSystem;

    @Column(columnDefinition = "TEXT")
    private String taxBreakdownJson;

    private BigDecimal totalTax;
    private BigDecimal grandTotal;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
