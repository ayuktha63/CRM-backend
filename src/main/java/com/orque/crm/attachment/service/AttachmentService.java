package com.orque.crm.attachment.service;

import com.orque.crm.attachment.entity.Attachment;
import com.orque.crm.attachment.repository.AttachmentRepository;
import com.orque.crm.timeline.service.TimelineService;
import com.orque.crm.common.UserContextHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class AttachmentService {

    private final AttachmentRepository repository;
    private final TimelineService timelineService;

    private final String uploadDir = "uploads";

    @Transactional
    public Attachment uploadFile(String moduleName, Long recordId, MultipartFile file) throws IOException {
        String username = UserContextHelper.currentUsername();
        if (username == null || username.isBlank()) {
            username = "system";
        }

        // Ensure upload directory exists
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null) {
            originalFileName = "unnamed_file";
        }
        
        // Generate unique local file path to prevent collision
        String storageName = System.currentTimeMillis() + "_" + originalFileName;
        Path targetPath = Paths.get(uploadDir, storageName);
        Files.copy(file.getInputStream(), targetPath);

        // Map relative URL
        String fileUrl = "/api/v1/attachments/download/" + storageName;

        // Compute version history (increment if same name exists for this record)
        List<Attachment> existing = repository.findByModuleNameAndRecordIdOrderByCreatedAtDesc(moduleName.toLowerCase(), recordId);
        int version = 1;
        for (Attachment att : existing) {
            if (att.getFileName().equalsIgnoreCase(originalFileName)) {
                version = att.getVersion() + 1;
                break;
            }
        }

        Attachment attachment = Attachment.builder()
                .moduleName(moduleName.toLowerCase())
                .recordId(recordId)
                .fileName(originalFileName)
                .fileSize(file.getSize())
                .contentType(file.getContentType() != null ? file.getContentType() : "application/octet-stream")
                .fileUrl(fileUrl)
                .version(version)
                .createdBy(username)
                .build();

        Attachment saved = repository.save(attachment);

        // Log to timeline
        timelineService.record(moduleName, recordId, "Attachment Added", 
                "Uploaded \"" + originalFileName + "\" (Version " + version + ") by " + username);

        return saved;
    }

    public List<Attachment> getAttachments(String moduleName, Long recordId) {
        return repository.findByModuleNameAndRecordIdOrderByCreatedAtDesc(moduleName.toLowerCase(), recordId);
    }

    public Path getFilePath(String storageName) {
        return Paths.get(uploadDir, storageName);
    }

    @Transactional
    public void deleteAttachment(Long id) {
        Attachment attachment = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Attachment not found"));
        
        // Try deleting local file
        try {
            String[] split = attachment.getFileUrl().split("/");
            String storageName = split[split.length - 1];
            Path targetPath = Paths.get(uploadDir, storageName);
            Files.deleteIfExists(targetPath);
        } catch (Exception e) {
            // Ignore file deletion errors
        }

        repository.delete(attachment);

        // Log to timeline
        timelineService.record(attachment.getModuleName(), attachment.getRecordId(), "Attachment Deleted", 
                "Deleted file \"" + attachment.getFileName() + "\"");
    }
}
