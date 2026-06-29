package com.orque.crm.tag.controller;

import com.orque.crm.tag.entity.Tag;
import com.orque.crm.tag.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/tags")
@RequiredArgsConstructor
@CrossOrigin
public class TagController {

    private final TagService service;

    @GetMapping
    public ResponseEntity<List<Tag>> getAllTags() {
        return ResponseEntity.ok(service.getAllTags());
    }

    @PostMapping
    public ResponseEntity<Tag> createTag(@RequestBody Tag tagDto) {
        return ResponseEntity.ok(service.createTag(tagDto.getName(), tagDto.getColorHex()));
    }

    @GetMapping("/{moduleName}/{recordId}")
    public ResponseEntity<List<Tag>> getTagsForRecord(
            @PathVariable String moduleName,
            @PathVariable Long recordId) {
        return ResponseEntity.ok(service.getTagsForRecord(moduleName, recordId));
    }

    @PostMapping("/{moduleName}/{recordId}/{tagId}")
    public ResponseEntity<Void> tagRecord(
            @PathVariable String moduleName,
            @PathVariable Long recordId,
            @PathVariable Long tagId) {
        service.tagRecord(moduleName, recordId, tagId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{moduleName}/{recordId}/{tagId}")
    public ResponseEntity<Void> untagRecord(
            @PathVariable String moduleName,
            @PathVariable Long recordId,
            @PathVariable Long tagId) {
        service.untagRecord(moduleName, recordId, tagId);
        return ResponseEntity.ok().build();
    }
}
