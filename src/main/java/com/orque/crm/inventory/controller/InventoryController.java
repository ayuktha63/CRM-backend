package com.orque.crm.inventory.controller;

import com.orque.crm.inventory.entity.*;
import com.orque.crm.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
@CrossOrigin
public class InventoryController {

    private final InventoryService service;

    // ── Vendors ──
    @GetMapping("/vendors")
    public ResponseEntity<List<Vendor>> getVendors() {
        return ResponseEntity.ok(service.getVendors());
    }

    @PostMapping("/vendors")
    public ResponseEntity<Vendor> saveVendor(@RequestBody Vendor vendor) {
        return ResponseEntity.ok(service.saveVendor(vendor));
    }

    // ── Price Books ──
    @GetMapping("/price-books")
    public ResponseEntity<List<PriceBook>> getPriceBooks() {
        return ResponseEntity.ok(service.getPriceBooks());
    }

    @PostMapping("/price-books")
    public ResponseEntity<PriceBook> savePriceBook(@RequestBody PriceBook book) {
        return ResponseEntity.ok(service.savePriceBook(book));
    }

    @PostMapping("/price-books/entries")
    public ResponseEntity<PriceBookEntry> savePriceBookEntry(@RequestBody PriceBookEntry entry) {
        return ResponseEntity.ok(service.savePriceBookEntry(entry));
    }

    // ── Warehouses ──
    @GetMapping("/warehouses")
    public ResponseEntity<List<Warehouse>> getWarehouses() {
        return ResponseEntity.ok(service.getWarehouses());
    }

    @PostMapping("/warehouses")
    public ResponseEntity<Warehouse> saveWarehouse(@RequestBody Warehouse warehouse) {
        return ResponseEntity.ok(service.saveWarehouse(warehouse));
    }

    @GetMapping("/warehouses/{id}/stock")
    public ResponseEntity<List<WarehouseStock>> getWarehouseStock(@PathVariable Long id) {
        return ResponseEntity.ok(service.getWarehouseStock(id));
    }

    @PostMapping("/warehouses/{id}/stock/adjust")
    public ResponseEntity<Void> adjustStock(
            @PathVariable Long id,
            @RequestParam Long productId,
            @RequestParam Integer adjustment,
            @RequestParam String reason) {
        service.adjustStock(id, productId, adjustment, reason);
        return ResponseEntity.ok().build();
    }

    // ── Purchase Orders ──
    @GetMapping("/purchase-orders")
    public ResponseEntity<List<PurchaseOrder>> getPurchaseOrders() {
        return ResponseEntity.ok(service.getPurchaseOrders());
    }

    @PostMapping("/purchase-orders")
    public ResponseEntity<PurchaseOrder> savePurchaseOrder(@RequestBody PurchaseOrder po) {
        String username = "system";
        try {
            username = com.orque.crm.common.UserContextHelper.currentUsername();
        } catch (Exception e) {
            // fallback
        }
        return ResponseEntity.ok(service.savePurchaseOrder(po, username));
    }

    // ── Sales Orders ──
    @GetMapping("/sales-orders")
    public ResponseEntity<List<SalesOrder>> getSalesOrders() {
        return ResponseEntity.ok(service.getSalesOrders());
    }

    @PostMapping("/sales-orders")
    public ResponseEntity<SalesOrder> saveSalesOrder(@RequestBody SalesOrder so) {
        String username = "system";
        try {
            username = com.orque.crm.common.UserContextHelper.currentUsername();
        } catch (Exception e) {
            // fallback
        }
        return ResponseEntity.ok(service.saveSalesOrder(so, username));
    }
}
