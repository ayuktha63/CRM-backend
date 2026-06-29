package com.orque.crm.note.controller;

import com.orque.crm.note.entity.Note;
import com.orque.crm.note.service.NoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/notes")
@RequiredArgsConstructor
@CrossOrigin
public class NoteController {

    private final NoteService service;

    @GetMapping("/{moduleName}/{recordId}")
    public ResponseEntity<List<Note>> getNotes(
            @PathVariable String moduleName,
            @PathVariable Long recordId) {
        return ResponseEntity.ok(service.getNotes(moduleName, recordId));
    }

    @PostMapping("/{moduleName}/{recordId}")
    public ResponseEntity<Note> createNote(
            @PathVariable String moduleName,
            @PathVariable Long recordId,
            @RequestBody String content) {
        // Strip extra quotes if payload was plain string
        if (content.startsWith("\"") && content.endsWith("\"")) {
            content = content.substring(1, content.length() - 1);
        }
        return ResponseEntity.ok(service.createNote(moduleName, recordId, content));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Note> updateNote(
            @PathVariable Long id,
            @RequestBody String content) {
        if (content.startsWith("\"") && content.endsWith("\"")) {
            content = content.substring(1, content.length() - 1);
        }
        return ResponseEntity.ok(service.updateNote(id, content));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNote(@PathVariable Long id) {
        service.deleteNote(id);
        return ResponseEntity.noContent().build();
    }
}
