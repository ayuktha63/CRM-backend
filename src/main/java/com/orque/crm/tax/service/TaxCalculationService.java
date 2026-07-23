package com.orque.crm.tax.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orque.crm.tax.dto.TaxBreakdown;
import com.orque.crm.tax.dto.TaxComponent;
import com.orque.crm.tax.entity.OrganizationTaxSettings;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Single source of truth for tax calculation across Quotes, Invoices, Sales Orders,
 * POS, reports, dashboards and exports. Every module MUST go through here rather than
 * computing tax inline — this is what makes changing a tenant's country/tax config
 * take effect everywhere at once, without hunting down duplicated 18%-GST style
 * constants scattered across the codebase (which is exactly what existed before this).
 *
 * Reads country-specific rules from configJson only — never branches on a country
 * name — so a new supported country is a data change (frontend list + one save),
 * never a code change here.
 *
 * Fallback: if a tenant hasn't configured tax settings yet, applies a flat 18% GST
 * (the CRM's original hardcoded behavior) so existing quotes/invoices keep working
 * unchanged until the tenant configures their real country.
 */
@Service
@RequiredArgsConstructor
public class TaxCalculationService {

    private static final BigDecimal FALLBACK_RATE = new BigDecimal("18");
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TaxBreakdown calculate(OrganizationTaxSettings settings, String customerState, BigDecimal subtotal) {
        BigDecimal base = subtotal != null ? subtotal : BigDecimal.ZERO;

        if (settings == null || settings.getConfigJson() == null || settings.getConfigJson().isBlank()) {
            return flatBreakdown(base, "GST", List.of(new RateSpec("GST", FALLBACK_RATE)));
        }

        try {
            JsonNode config = objectMapper.readTree(settings.getConfigJson());
            List<RateSpec> components = resolveComponents(config, settings.getBusinessState(), customerState);
            return flatBreakdown(base, settings.getTaxSystem(), components);
        } catch (Exception e) {
            // Malformed config shouldn't block invoicing — fall back rather than 500.
            return flatBreakdown(base, "GST", List.of(new RateSpec("GST", FALLBACK_RATE)));
        }
    }

    private List<RateSpec> resolveComponents(JsonNode config, String businessState, String customerState) {
        JsonNode flat = config.get("flatComponents");
        if (flat != null && !flat.isNull() && flat.isArray() && !flat.isEmpty()) {
            return toRateSpecs(flat);
        }

        boolean sameState = businessState != null && !businessState.isBlank() && customerState != null
                && businessState.trim().equalsIgnoreCase(customerState.trim());

        JsonNode node = sameState ? config.get("sameStateComponents") : config.get("differentStateComponents");
        if (node != null && !node.isNull() && node.isArray() && !node.isEmpty()) {
            return toRateSpecs(node);
        }
        return List.of(new RateSpec("TAX", FALLBACK_RATE));
    }

    private List<RateSpec> toRateSpecs(JsonNode arrayNode) {
        List<RateSpec> specs = new ArrayList<>();
        for (JsonNode n : arrayNode) {
            String name = n.has("name") ? n.get("name").asText() : "TAX";
            BigDecimal rate = n.has("rate") ? new BigDecimal(n.get("rate").asText()) : BigDecimal.ZERO;
            specs.add(new RateSpec(name, rate));
        }
        return specs;
    }

    private TaxBreakdown flatBreakdown(BigDecimal subtotal, String taxSystem, List<RateSpec> components) {
        List<TaxComponent> taxes = new ArrayList<>();
        BigDecimal totalTax = BigDecimal.ZERO;
        for (RateSpec spec : components) {
            BigDecimal amount = subtotal.multiply(spec.rate)
                    .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
                    .setScale(2, RoundingMode.HALF_UP);
            taxes.add(TaxComponent.builder().name(spec.name).rate(spec.rate).amount(amount).build());
            totalTax = totalTax.add(amount);
        }
        return TaxBreakdown.builder()
                .subtotal(subtotal.setScale(2, RoundingMode.HALF_UP))
                .taxes(taxes)
                .totalTax(totalTax)
                .grandTotal(subtotal.add(totalTax).setScale(2, RoundingMode.HALF_UP))
                .taxSystem(taxSystem != null ? taxSystem : "GST")
                .build();
    }

    private record RateSpec(String name, BigDecimal rate) {}
}
