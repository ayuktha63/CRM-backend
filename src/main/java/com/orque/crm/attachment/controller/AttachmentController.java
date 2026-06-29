package com.orque.crm.attachment.controller;

import com.orque.crm.attachment.entity.Attachment;
import com.orque.crm.attachment.service.AttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/v1/attachments")
@RequiredArgsConstructor
@CrossOrigin
public class AttachmentController {

    private final AttachmentService service;

    @GetMapping("/{moduleName}/{recordId}")
    public ResponseEntity<List<Attachment>> getAttachments(
            @PathVariable String moduleName,
            @PathVariable Long recordId) {
        return ResponseEntity.ok(service.getAttachments(moduleName, recordId));
    }

    @PostMapping("/{moduleName}/{recordId}")
    public ResponseEntity<Attachment> uploadAttachment(
            @PathVariable String moduleName,
            @PathVariable Long recordId,
            @RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(service.uploadFile(moduleName, recordId, file));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAttachment(@PathVariable Long id) {
        service.deleteAttachment(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/download/{storageName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String storageName) {
        try {
            Path path = service.getFilePath(storageName);
            Resource resource = new UrlResource(path.toUri());

            if (resource.exists() || resource.isReadable()) {
                String originalName = resource.getFilename();
                if (originalName != null && originalName.length() > 14) {
                    originalName = originalName.substring(14);
                }
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + originalName + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
