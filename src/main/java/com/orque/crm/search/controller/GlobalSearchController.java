package com.orque.crm.search.controller;

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

    @GetMapping
    public ResponseEntity<Map<String, List<Map<String, Object>>>> search(
            @RequestParam("q") String query) {
        return ResponseEntity.ok(service.searchAll(query));
    }
}
