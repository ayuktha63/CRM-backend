package com.orque.crm.feature.controller;

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
        return ResponseEntity.ok(productRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getById(@PathVariable Long id) {
        return productRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Product> save(@RequestBody Product product) {
        if (product.getId() != null) {
            Product existing = productRepository.findById(product.getId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));
            existing.setName(product.getName());
            existing.setSku(product.getSku());
            existing.setCategory(product.getCategory());
            existing.setPrice(product.getPrice());
            existing.setStock(product.getStock());
            existing.setTaxRate(product.getTaxRate());
            existing.setStatus(product.getStatus());
            return ResponseEntity.ok(productRepository.save(existing));
        }
        return ResponseEntity.ok(productRepository.save(product));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> update(@PathVariable Long id, @RequestBody Product product) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        existing.setName(product.getName());
        existing.setSku(product.getSku());
        existing.setCategory(product.getCategory());
        existing.setPrice(product.getPrice());
        existing.setStock(product.getStock());
        existing.setTaxRate(product.getTaxRate());
        existing.setStatus(product.getStatus());
        return ResponseEntity.ok(productRepository.save(existing));
    }

    @PostMapping("/deactivate/{id}")
    public ResponseEntity<Product> deactivate(@PathVariable Long id) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        existing.setStatus("Inactive");
        return ResponseEntity.ok(productRepository.save(existing));
    }

    @PostMapping("/activate/{id}")
    public ResponseEntity<Product> activate(@PathVariable Long id) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        existing.setStatus("Active");
        return ResponseEntity.ok(productRepository.save(existing));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        productRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Product deleted successfully"));
    }
}
