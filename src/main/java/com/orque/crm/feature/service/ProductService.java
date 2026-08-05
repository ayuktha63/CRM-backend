package com.orque.crm.feature.service;

import com.orque.crm.common.UserContextHelper;
import com.orque.crm.feature.entity.Product;
import com.orque.crm.feature.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Business logic for products. Follows the leads bulk-import pattern: each row
 * is normalised and imported independently, so one bad row doesn't fail the
 * entire import.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    /**
     * Bulk-imports a list of products. Rows that fail to persist (e.g. missing
     * required fields) are logged and skipped, mirroring
     * {@code LeadServiceImpl#bulkImportLeads}.
     */
    public List<Product> bulkImportProducts(List<Product> requests) {
        List<Product> imported = new ArrayList<>();
        int rowIndex = 0;
        for (Product request : requests) {
            rowIndex++;
            try {
                imported.add(saveProduct(request, rowIndex));
            } catch (RuntimeException e) {
                log.warn("Bulk import: skipped product row {}: {}", rowIndex, e.getMessage());
            }
        }
        return imported;
    }

    private Product saveProduct(Product request, int rowIndex) {
        // Never trust client-supplied identity or timestamps — imports always create new rows.
        request.setId(null);
        request.setCreatedAt(null);
        request.setUpdatedAt(null);
        request.setOrganizationId(UserContextHelper.currentOrganizationId());

        // Defaults for optional fields so the row can be persisted.
        if (request.getName() == null || request.getName().isBlank()) {
            request.setName("Untitled Product " + rowIndex);
        }
        if (request.getStatus() == null || request.getStatus().isBlank()) {
            request.setStatus("Active");
        }

        return productRepository.save(request);
    }
}
