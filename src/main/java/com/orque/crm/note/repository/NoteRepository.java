package com.orque.crm.note.repository;

import com.orque.crm.note.entity.Note;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NoteRepository extends JpaRepository<Note, Long> {
    List<Note> findByModuleNameAndRecordIdOrderByCreatedAtDesc(String moduleName, Long recordId);
}
