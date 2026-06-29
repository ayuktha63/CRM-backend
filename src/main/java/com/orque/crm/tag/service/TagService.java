package com.orque.crm.tag.service;

import com.orque.crm.tag.entity.Tag;
import com.orque.crm.tag.entity.RecordTag;
import com.orque.crm.tag.repository.TagRepository;
import com.orque.crm.tag.repository.RecordTagRepository;
import com.orque.crm.timeline.service.TimelineService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;
    private final RecordTagRepository recordTagRepository;
    private final TimelineService timelineService;

    public List<Tag> getAllTags() {
        return tagRepository.findAll();
    }

    @Transactional
    public Tag createTag(String name, String colorHex) {
        Optional<Tag> existing = tagRepository.findByNameIgnoreCase(name);
        if (existing.isPresent()) {
            return existing.get();
        }
        Tag tag = Tag.builder()
                .name(name.trim())
                .colorHex(colorHex)
                .build();
        return tagRepository.save(tag);
    }

    @Transactional
    public void tagRecord(String moduleName, Long recordId, Long tagId) {
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new NoSuchElementException("Tag not found"));

        Optional<RecordTag> existing = recordTagRepository.findByModuleNameAndRecordIdAndTagId(
                moduleName.toLowerCase(), recordId, tagId);
        
        if (existing.isEmpty()) {
            RecordTag association = RecordTag.builder()
                    .moduleName(moduleName.toLowerCase())
                    .recordId(recordId)
                    .tagId(tagId)
                    .build();
            recordTagRepository.save(association);

            // Log to timeline
            timelineService.record(moduleName, recordId, "Tag Added", "Tagged with label \"" + tag.getName() + "\"");
        }
    }

    @Transactional
    public void untagRecord(String moduleName, Long recordId, Long tagId) {
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new NoSuchElementException("Tag not found"));

        Optional<RecordTag> existing = recordTagRepository.findByModuleNameAndRecordIdAndTagId(
                moduleName.toLowerCase(), recordId, tagId);
        
        existing.ifPresent(recordTag -> {
            recordTagRepository.delete(recordTag);
            // Log to timeline
            timelineService.record(moduleName, recordId, "Tag Removed", "Removed label \"" + tag.getName() + "\"");
        });
    }

    public List<Tag> getTagsForRecord(String moduleName, Long recordId) {
        List<RecordTag> associations = recordTagRepository.findByModuleNameAndRecordId(
                moduleName.toLowerCase(), recordId);
        List<Long> tagIds = associations.stream().map(RecordTag::getTagId).collect(Collectors.toList());
        return tagRepository.findAllById(tagIds);
    }
}
