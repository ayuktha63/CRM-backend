package com.orque.crm.inventory.service;

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

    // ── Vendor Actions ──
    public List<Vendor> getVendors() { return vendorRepository.findAll(); }
    
    @Transactional
    public Vendor saveVendor(Vendor vendor) { return vendorRepository.save(vendor); }

    // ── Price Book Actions ──
    public List<PriceBook> getPriceBooks() { return priceBookRepository.findAll(); }
    
    @Transactional
    public PriceBook savePriceBook(PriceBook book) { return priceBookRepository.save(book); }

    @Transactional
    public PriceBookEntry savePriceBookEntry(PriceBookEntry entry) { return priceBookEntryRepository.save(entry); }

    public BigDecimal resolveProductPrice(Long priceBookId, Long productId, BigDecimal defaultPrice) {
        if (priceBookId == null || priceBookId <= 0) return defaultPrice;
        return priceBookEntryRepository.findByPriceBookIdAndProductId(priceBookId, productId)
                .map(PriceBookEntry::getCustomPrice)
                .orElse(defaultPrice);
    }

    // ── Warehouse Stock Actions ──
    public List<Warehouse> getWarehouses() { return warehouseRepository.findAll(); }
    
    @Transactional
    public Warehouse saveWarehouse(Warehouse warehouse) { return warehouseRepository.save(warehouse); }

    public List<WarehouseStock> getWarehouseStock(Long warehouseId) {
        return warehouseStockRepository.findByWarehouseId(warehouseId);
    }

    @Transactional
    public void adjustStock(Long warehouseId, Long productId, Integer quantityAdjustment, String actionName) {
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
    public List<PurchaseOrder> getPurchaseOrders() { return purchaseOrderRepository.findAll(); }

    @Transactional
    public PurchaseOrder savePurchaseOrder(PurchaseOrder po, String username) {
        po.setCreatedBy(username);
        PurchaseOrder saved = purchaseOrderRepository.save(po);

        // If PO status changed to RECEIVED, auto-increment stock levels
        if ("RECEIVED".equalsIgnoreCase(saved.getStatus())) {
            // Assume default warehouse ID 1 for simple auto-updates
            adjustStock(1L, 1L, 100, "Purchase Order " + saved.getPoNumber() + " Received");
        }
        return saved;
    }

    // ── Sales Order Actions ──
    public List<SalesOrder> getSalesOrders() { return salesOrderRepository.findAll(); }

    @Transactional
    public SalesOrder saveSalesOrder(SalesOrder so, String username) {
        so.setCreatedBy(username);
        SalesOrder saved = salesOrderRepository.save(so);

        // If SO status changed to SHIPPED, auto-decrement stock levels
        if ("SHIPPED".equalsIgnoreCase(saved.getStatus())) {
            adjustStock(1L, 1L, -50, "Sales Order " + saved.getSoNumber() + " Shipped");
        }
        return saved;
    }
}
