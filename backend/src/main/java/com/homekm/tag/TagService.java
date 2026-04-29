package com.homekm.tag;

import com.homekm.auth.UserPrincipal;
import com.homekm.common.ChildAccountWriteException;
import com.homekm.common.EntityNotFoundException;
import com.homekm.tag.dto.TagRequest;
import com.homekm.tag.dto.TagResponse;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Service
public class TagService {

    private final TagRepository tagRepository;
    private final TaggingRepository taggingRepository;

    public TagService(TagRepository tagRepository, TaggingRepository taggingRepository) {
        this.tagRepository = tagRepository;
        this.taggingRepository = taggingRepository;
    }

    @Cacheable("tags")
    public List<TagResponse> list() {
        return tagRepository.findAll().stream().map(TagResponse::from).toList();
    }

    public List<TagResponse> autocomplete(String q) {
        return tagRepository.findByNameContaining(q).stream().map(TagResponse::from).toList();
    }

    /**
     * Move every tagging from {@code sourceId} to {@code targetId} and delete
     * the source. Adults only. Idempotent at the row level (already-tagged
     * entities are skipped, leftover source rows are wiped).
     */
    @Transactional
    @CacheEvict(value = "tags", allEntries = true)
    public void merge(Long sourceId, Long targetId, UserPrincipal principal) {
        if (principal.isChild()) throw new ChildAccountWriteException();
        if (sourceId.equals(targetId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SAME_TAG");
        }
        Tag source = tagRepository.findById(sourceId)
                .orElseThrow(() -> new EntityNotFoundException("Tag", sourceId));
        if (!tagRepository.existsById(targetId)) {
            throw new EntityNotFoundException("Tag", targetId);
        }
        taggingRepository.moveTaggings(sourceId, targetId);
        taggingRepository.deleteByTagId(sourceId);
        tagRepository.delete(source);
    }

    @Transactional
    @CacheEvict(value = "tags", allEntries = true)
    public TagResponse create(TagRequest req, UserPrincipal principal) {
        if (principal.isChild()) throw new ChildAccountWriteException();
        if (tagRepository.existsByNameIgnoreCase(req.name())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "TAG_NAME_EXISTS");
        }
        Tag tag = new Tag();
        tag.setName(req.name());
        tag.setColor(req.color() != null ? req.color() : "#6366f1");
        tagRepository.save(tag);
        return TagResponse.from(tag);
    }

    @Transactional
    @CacheEvict(value = "tags", allEntries = true)
    public TagResponse update(Long id, TagRequest req, UserPrincipal principal) {
        if (principal.isChild()) throw new ChildAccountWriteException();
        Tag tag = tagRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Tag", id));
        if (req.name() != null && !req.name().equalsIgnoreCase(tag.getName())) {
            if (tagRepository.existsByNameIgnoreCase(req.name())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "TAG_NAME_EXISTS");
            }
            tag.setName(req.name());
        }
        if (req.color() != null) tag.setColor(req.color());
        tagRepository.save(tag);
        return TagResponse.from(tag);
    }

    @Transactional
    @CacheEvict(value = "tags", allEntries = true)
    public void delete(Long id, UserPrincipal principal) {
        if (principal.isChild()) throw new ChildAccountWriteException();
        Tag tag = tagRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Tag", id));
        taggingRepository.deleteAll(taggingRepository.findByEntityTypeAndEntityId("note", id));
        taggingRepository.deleteAll(taggingRepository.findByEntityTypeAndEntityId("file", id));
        taggingRepository.deleteAll(taggingRepository.findByEntityTypeAndEntityId("folder", id));
        tagRepository.delete(tag);
    }

    @Transactional
    public List<TagResponse> attachTags(String entityType, Long entityId,
                                         List<Long> tagIds, UserPrincipal principal) {
        if (principal.isChild()) throw new ChildAccountWriteException();
        long current = taggingRepository.countByEntityTypeAndEntityId(entityType, entityId);
        for (Long tagId : tagIds) {
            if (taggingRepository.existsByTagIdAndEntityTypeAndEntityId(tagId, entityType, entityId)) continue;
            if (current >= 20) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MAX_TAGS_EXCEEDED");
            Tag tag = tagRepository.findById(tagId).orElseThrow(() -> new EntityNotFoundException("Tag", tagId));
            Tagging tagging = new Tagging();
            tagging.setTag(tag);
            tagging.setEntityType(entityType);
            tagging.setEntityId(entityId);
            taggingRepository.save(tagging);
            current++;
        }
        return getTagsForEntity(entityType, entityId);
    }

    @Transactional
    public void detachTag(String entityType, Long entityId, Long tagId, UserPrincipal principal) {
        if (principal.isChild()) throw new ChildAccountWriteException();
        taggingRepository.findByTagIdAndEntityTypeAndEntityId(tagId, entityType, entityId)
                .ifPresent(taggingRepository::delete);
    }

    public List<TagResponse> getTagsForEntity(String entityType, Long entityId) {
        return taggingRepository.findByEntityTypeAndEntityId(entityType, entityId)
                .stream().map(t -> TagResponse.from(t.getTag())).toList();
    }

    public record BulkItem(String entityType, Long entityId) {}

    /**
     * Apply {@code addTagIds} and remove {@code removeTagIds} across every
     * item in one transaction. Skips no-op operations (already-attached
     * adds, already-absent removes). Adults only. Returns the number of
     * (item, tag) pairs that were actually mutated.
     */
    @Transactional
    @CacheEvict(value = "tags", allEntries = true)
    public int bulkUpdateTaggings(List<BulkItem> items,
                                   List<Long> addTagIds,
                                   List<Long> removeTagIds,
                                   UserPrincipal principal) {
        if (principal.isChild()) throw new ChildAccountWriteException();
        if (items == null || items.isEmpty()) return 0;
        int mutated = 0;
        for (BulkItem item : items) {
            if (addTagIds != null) {
                long current = taggingRepository.countByEntityTypeAndEntityId(item.entityType(), item.entityId());
                for (Long tagId : addTagIds) {
                    if (taggingRepository.existsByTagIdAndEntityTypeAndEntityId(tagId, item.entityType(), item.entityId())) continue;
                    if (current >= 20) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MAX_TAGS_EXCEEDED");
                    Tag tag = tagRepository.findById(tagId)
                            .orElseThrow(() -> new EntityNotFoundException("Tag", tagId));
                    Tagging t = new Tagging();
                    t.setTag(tag);
                    t.setEntityType(item.entityType());
                    t.setEntityId(item.entityId());
                    taggingRepository.save(t);
                    current++;
                    mutated++;
                }
            }
            if (removeTagIds != null) {
                for (Long tagId : removeTagIds) {
                    var existing = taggingRepository.findByTagIdAndEntityTypeAndEntityId(tagId, item.entityType(), item.entityId());
                    if (existing.isPresent()) {
                        taggingRepository.delete(existing.get());
                        mutated++;
                    }
                }
            }
        }
        return mutated;
    }
}
