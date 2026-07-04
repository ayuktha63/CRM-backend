package com.orque.crm.feature.controller;

import com.orque.crm.common.UserContextHelper;
import com.orque.crm.feature.entity.Product;
import com.orque.crm.feature.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@CrossOrigin
public class ProductController {

    private final ProductRepository productRepository;

    @GetMapping
    public ResponseEntity<List<Product>> getAll() {
        String orgId = UserContextHelper.scopedOrgId();
        if (orgId == null) return ResponseEntity.ok(productRepository.findAll());
        return ResponseEntity.ok(productRepository.findByOrganizationId(orgId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getById(@PathVariable Long id) {
        return productRepository.findById(id)
                .map(p -> {
                    if (!inScope(p.getOrganizationId())) return ResponseEntity.status(403).<Product>build();
                    return ResponseEntity.ok(p);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Product> save(@RequestBody Product product) {
        if (product.getId() != null) {
            Product existing = productRepository.findById(product.getId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));
            if (!inScope(existing.getOrganizationId())) return ResponseEntity.status(403).build();
            existing.setName(product.getName());
            existing.setSku(product.getSku());
            existing.setCategory(product.getCategory());
            existing.setPrice(product.getPrice());
            existing.setStock(product.getStock());
            existing.setTaxRate(product.getTaxRate());
            existing.setStatus(product.getStatus());
            return ResponseEntity.ok(productRepository.save(existing));
        }
        product.setOrganizationId(UserContextHelper.currentOrganizationId());
        return ResponseEntity.ok(productRepository.save(product));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> update(@PathVariable Long id, @RequestBody Product product) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        if (!inScope(existing.getOrganizationId())) return ResponseEntity.status(403).build();
        existing.setName(product.getName());
        existing.setSku(product.getSku());
        existing.setCategory(product.getCategory());
        existing.setPrice(product.getPrice());
        existing.setStock(product.getStock());
        existing.setTaxRate(product.getTaxRate());
        existing.setStatus(product.getStatus());
        return ResponseEntity.ok(productRepository.save(existing));
    }

    // Path is {id}/deactivate (not deactivate/{id}) to match the frontend's generic
    // `${base}/${uuid}/${action}` convention — the previous ordering 404'd every call.
    @PostMapping("/{id}/deactivate")
    public ResponseEntity<Product> deactivate(@PathVariable Long id) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        if (!inScope(existing.getOrganizationId())) return ResponseEntity.status(403).build();
        existing.setStatus("Inactive");
        return ResponseEntity.ok(productRepository.save(existing));
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<Product> activate(@PathVariable Long id) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        if (!inScope(existing.getOrganizationId())) return ResponseEntity.status(403).build();
        existing.setStatus("Active");
        return ResponseEntity.ok(productRepository.save(existing));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        if (!inScope(existing.getOrganizationId())) return ResponseEntity.status(403).build();
        productRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Product deleted successfully"));
    }

    private boolean inScope(String recordOrgId) {
        return UserContextHelper.isSystemAdmin()
                || recordOrgId == null
                || recordOrgId.equals(UserContextHelper.currentOrganizationId());
    }
}
