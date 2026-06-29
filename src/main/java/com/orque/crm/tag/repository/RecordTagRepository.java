package com.orque.crm.tag.repository;

import com.orque.crm.tag.entity.RecordTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface RecordTagRepository extends JpaRepository<RecordTag, Long> {
    List<RecordTag> findByModuleNameAndRecordId(String moduleName, Long recordId);
    Optional<RecordTag> findByModuleNameAndRecordIdAndTagId(String moduleName, Long recordId, Long tagId);
    List<RecordTag> findByTagId(Long tagId);
}
