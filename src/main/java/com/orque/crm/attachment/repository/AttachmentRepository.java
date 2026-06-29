package com.orque.crm.attachment.repository;

import com.orque.crm.attachment.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    List<Attachment> findByModuleNameAndRecordIdOrderByCreatedAtDesc(String moduleName, Long recordId);
}
