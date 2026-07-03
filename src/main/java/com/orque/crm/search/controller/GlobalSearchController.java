package com.orque.crm.search.controller;

import com.orque.crm.config.query.QueryMapping;
import com.orque.crm.search.dto.SearchQueryRequest;
import com.orque.crm.search.service.GlobalSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
@CrossOrigin
public class GlobalSearchController {

    private final GlobalSearchService service;

    /**
     * HTTP QUERY /api/v1/search (RFC 10008) — safe and idempotent full-text search
     * across all CRM entities. Accepts a JSON body instead of a query string to
     * avoid URI length limits and enable future complex filter extensions.
     *
     * Body: { "q": "search term" }
     */
    @QueryMapping
    public ResponseEntity<Map<String, List<Map<String, Object>>>> search(
            @RequestBody SearchQueryRequest request) {
        return ResponseEntity.ok(service.searchAll(request.getQ()));
    }
}
