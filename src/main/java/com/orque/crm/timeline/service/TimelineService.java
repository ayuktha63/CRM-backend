package com.orque.crm.timeline.service;

import com.orque.crm.timeline.entity.TimelineEntry;
import com.orque.crm.timeline.repository.TimelineRepository;
import com.orque.crm.common.UserContextHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TimelineService {

    private final TimelineRepository repository;

    public List<TimelineEntry> getTimeline(String moduleName, Long recordId) {
        return repository.findByModuleNameAndRecordIdOrderByCreatedAtDesc(moduleName.toLowerCase(), recordId);
    }

    @Transactional
    public void record(String moduleName, Long recordId, String action, String description) {
        String username = UserContextHelper.currentUsername();
        if (username == null || username.isBlank()) {
            username = "system";
        }
        TimelineEntry entry = TimelineEntry.builder()
                .moduleName(moduleName.toLowerCase())
                .recordId(recordId)
                .action(action)
                .description(description)
                .performedBy(username)
                .build();
        repository.save(entry);
    }
}
