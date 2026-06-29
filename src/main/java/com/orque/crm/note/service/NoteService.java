package com.orque.crm.note.service;

import com.orque.crm.note.entity.Note;
import com.orque.crm.note.repository.NoteRepository;
import com.orque.crm.timeline.service.TimelineService;
import com.orque.crm.common.UserContextHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class NoteService {

    private final NoteRepository repository;
    private final TimelineService timelineService;

    public List<Note> getNotes(String moduleName, Long recordId) {
        return repository.findByModuleNameAndRecordIdOrderByCreatedAtDesc(moduleName.toLowerCase(), recordId);
    }

    @Transactional
    public Note createNote(String moduleName, Long recordId, String content) {
        String username = UserContextHelper.currentUsername();
        if (username == null || username.isBlank()) {
            username = "system";
        }
        Note note = Note.builder()
                .moduleName(moduleName.toLowerCase())
                .recordId(recordId)
                .content(content)
                .createdBy(username)
                .build();
        Note saved = repository.save(note);
        
        // Log to timeline
        String snippet = content.length() > 50 ? content.substring(0, 47) + "..." : content;
        timelineService.record(moduleName, recordId, "Note Added", "\"" + snippet + "\" by " + username);
        
        return saved;
    }

    @Transactional
    public Note updateNote(Long id, String content) {
        Note note = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Note not found"));
        note.setContent(content);
        Note updated = repository.save(note);

        // Log to timeline
        String snippet = content.length() > 50 ? content.substring(0, 47) + "..." : content;
        timelineService.record(note.getModuleName(), note.getRecordId(), "Note Updated", "\"" + snippet + "\"");

        return updated;
    }

    @Transactional
    public void deleteNote(Long id) {
        Note note = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Note not found"));
        repository.delete(note);

        // Log to timeline
        timelineService.record(note.getModuleName(), note.getRecordId(), "Note Deleted", "Deleted note created by " + note.getCreatedBy());
    }
}
