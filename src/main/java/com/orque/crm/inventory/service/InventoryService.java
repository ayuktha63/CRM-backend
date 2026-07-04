package com.orque.crm.inventory.service;

import com.orque.crm.common.UserContextHelper;
import com.orque.crm.inventory.entity.*;
import com.orque.crm.inventory.repository.*;
import com.orque.crm.timeline.service.TimelineService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Inventory is tenant-shared, not per-user: every record is scoped by organizationId only
 * (single-tenant isolation), and any user within that tenant — including SYSTEM_ADMIN and
 * regular SALES_USER alike — can see, edit, or delete any record here. There is
 * deliberately no owner/createdBy-based filtering on top of the org scope.
 */
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final VendorRepository vendorRepository;
    private final PriceBookRepository priceBookRepository;
    private final PriceBookEntryRepository priceBookEntryRepository;
    private final WarehouseRepository warehouseRepository;
    private final WarehouseStockRepository warehouseStockRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final TimelineService timelineService;

    private String orgId() { return UserContextHelper.currentOrganizationId(); }

    /** Throws if a record belonging to another tenant is being read/edited/deleted. */
    private void assertSameOrg(String recordOrgId) {
        String myOrg = orgId();
        if (myOrg != null && recordOrgId != null && !myOrg.equals(recordOrgId)) {
            throw new SecurityException("Record does not belong to your organization.");
        }
    }

    // ── Vendor Actions ──
    public List<Vendor> getVendors() {
        String org = orgId();
        return org != null ? vendorRepository.findByOrganizationId(org) : vendorRepository.findAll();
    }

    @Transactional
    public Vendor saveVendor(Vendor vendor) {
        if (vendor.getId() != null) {
            Vendor existing = vendorRepository.findById(vendor.getId())
                    .orElseThrow(() -> new NoSuchElementException("Vendor not found: " + vendor.getId()));
            assertSameOrg(existing.getOrganizationId());
            vendor.setOrganizationId(existing.getOrganizationId());
        } else if (vendor.getOrganizationId() == null) {
            vendor.setOrganizationId(orgId());
        }
        return vendorRepository.save(vendor);
    }

    @Transactional
    public void deleteVendor(Long id) {
        Vendor existing = vendorRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Vendor not found: " + id));
        assertSameOrg(existing.getOrganizationId());
        vendorRepository.delete(existing);
    }

    // ── Price Book Actions ──
    public List<PriceBook> getPriceBooks() {
        String org = orgId();
        return org != null ? priceBookRepository.findByOrganizationId(org) : priceBookRepository.findAll();
    }

    @Transactional
    public PriceBook savePriceBook(PriceBook book) {
        if (book.getId() != null) {
            PriceBook existing = priceBookRepository.findById(book.getId())
                    .orElseThrow(() -> new NoSuchElementException("Price book not found: " + book.getId()));
            assertSameOrg(existing.getOrganizationId());
            book.setOrganizationId(existing.getOrganizationId());
        } else if (book.getOrganizationId() == null) {
            book.setOrganizationId(orgId());
        }
        return priceBookRepository.save(book);
    }

    @Transactional
    public void deletePriceBook(Long id) {
        PriceBook existing = priceBookRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Price book not found: " + id));
        assertSameOrg(existing.getOrganizationId());
        priceBookRepository.delete(existing);
    }

    @Transactional
    public PriceBookEntry savePriceBookEntry(PriceBookEntry entry) { return priceBookEntryRepository.save(entry); }

    public BigDecimal resolveProductPrice(Long priceBookId, Long productId, BigDecimal defaultPrice) {
        if (priceBookId == null || priceBookId <= 0) return defaultPrice;
        return priceBookEntryRepository.findByPriceBookIdAndProductId(priceBookId, productId)
                .map(PriceBookEntry::getCustomPrice)
                .orElse(defaultPrice);
    }

    // ── Warehouse Stock Actions ──
    public List<Warehouse> getWarehouses() {
        String org = orgId();
        return org != null ? warehouseRepository.findByOrganizationId(org) : warehouseRepository.findAll();
    }

    @Transactional
    public Warehouse saveWarehouse(Warehouse warehouse) {
        if (warehouse.getId() != null) {
            Warehouse existing = warehouseRepository.findById(warehouse.getId())
                    .orElseThrow(() -> new NoSuchElementException("Warehouse not found: " + warehouse.getId()));
            assertSameOrg(existing.getOrganizationId());
            warehouse.setOrganizationId(existing.getOrganizationId());
        } else if (warehouse.getOrganizationId() == null) {
            warehouse.setOrganizationId(orgId());
        }
        return warehouseRepository.save(warehouse);
    }

    @Transactional
    public void deleteWarehouse(Long id) {
        Warehouse existing = warehouseRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Warehouse not found: " + id));
        assertSameOrg(existing.getOrganizationId());
        warehouseRepository.delete(existing);
    }

    /** Verifies the warehouse belongs to the caller's org before returning its stock. */
    public List<WarehouseStock> getWarehouseStock(Long warehouseId) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new NoSuchElementException("Warehouse not found: " + warehouseId));
        assertSameOrg(warehouse.getOrganizationId());
        return warehouseStockRepository.findByWarehouseId(warehouseId);
    }

    @Transactional
    public void adjustStock(Long warehouseId, Long productId, Integer quantityAdjustment, String actionName) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new NoSuchElementException("Warehouse not found: " + warehouseId));
        assertSameOrg(warehouse.getOrganizationId());

        Optional<WarehouseStock> existing = warehouseStockRepository.findByWarehouseIdAndProductId(warehouseId, productId);
        if (existing.isPresent()) {
            WarehouseStock stock = existing.get();
            stock.setQuantity(Math.max(0, stock.getQuantity() + quantityAdjustment));
            warehouseStockRepository.save(stock);
        } else {
            WarehouseStock stock = WarehouseStock.builder()
                    .warehouseId(warehouseId)
                    .productId(productId)
                    .quantity(Math.max(0, quantityAdjustment))
                    .build();
            warehouseStockRepository.save(stock);
        }
        timelineService.record("products", productId, "Stock Adjusted",
                "Stock adjusted by " + quantityAdjustment + " units (" + actionName + ") in warehouse ID " + warehouseId);
    }

    // ── Purchase Order Actions ──
    public List<PurchaseOrder> getPurchaseOrders() {
        String org = orgId();
        return org != null ? purchaseOrderRepository.findByOrganizationId(org) : purchaseOrderRepository.findAll();
    }

    @Transactional
    public PurchaseOrder savePurchaseOrder(PurchaseOrder po, String username) {
        if (po.getId() != null) {
            PurchaseOrder existing = purchaseOrderRepository.findById(po.getId())
                    .orElseThrow(() -> new NoSuchElementException("Purchase order not found: " + po.getId()));
            assertSameOrg(existing.getOrganizationId());
            po.setOrganizationId(existing.getOrganizationId());
            po.setCreatedBy(existing.getCreatedBy());
        } else {
            po.setCreatedBy(username);
            if (po.getOrganizationId() == null) po.setOrganizationId(orgId());
        }
        PurchaseOrder saved = purchaseOrderRepository.save(po);

        // If PO status changed to RECEIVED, auto-increment stock levels
        if ("RECEIVED".equalsIgnoreCase(saved.getStatus())) {
            // Assume default warehouse ID 1 for simple auto-updates
            adjustStock(1L, 1L, 100, "Purchase Order " + saved.getPoNumber() + " Received");
        }
        return saved;
    }

    @Transactional
    public void deletePurchaseOrder(Long id) {
        PurchaseOrder existing = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Purchase order not found: " + id));
        assertSameOrg(existing.getOrganizationId());
        purchaseOrderRepository.delete(existing);
    }

    // ── Sales Order Actions ──
    public List<SalesOrder> getSalesOrders() {
        String org = orgId();
        return org != null ? salesOrderRepository.findByOrganizationId(org) : salesOrderRepository.findAll();
    }

    @Transactional
    public SalesOrder saveSalesOrder(SalesOrder so, String username) {
        if (so.getId() != null) {
            SalesOrder existing = salesOrderRepository.findById(so.getId())
                    .orElseThrow(() -> new NoSuchElementException("Sales order not found: " + so.getId()));
            assertSameOrg(existing.getOrganizationId());
            so.setOrganizationId(existing.getOrganizationId());
            so.setCreatedBy(existing.getCreatedBy());
        } else {
            so.setCreatedBy(username);
            if (so.getOrganizationId() == null) so.setOrganizationId(orgId());
        }
        SalesOrder saved = salesOrderRepository.save(so);

        // If SO status changed to SHIPPED, auto-decrement stock levels
        if ("SHIPPED".equalsIgnoreCase(saved.getStatus())) {
            adjustStock(1L, 1L, -50, "Sales Order " + saved.getSoNumber() + " Shipped");
        }
        return saved;
    }

    @Transactional
    public void deleteSalesOrder(Long id) {
        SalesOrder existing = salesOrderRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Sales order not found: " + id));
        assertSameOrg(existing.getOrganizationId());
        salesOrderRepository.delete(existing);
    }
}
