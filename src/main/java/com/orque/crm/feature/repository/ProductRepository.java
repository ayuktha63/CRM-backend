package com.orque.crm.feature.repository;

import com.orque.crm.feature.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    java.util.List<Product> findByOrganizationId(String organizationId);
}
