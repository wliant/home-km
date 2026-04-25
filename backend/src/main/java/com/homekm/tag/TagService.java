package com.homekm.tag;

import com.homekm.auth.UserPrincipal;
import com.homekm.common.ChildAccountWriteException;
import com.homekm.common.EntityNotFoundException;
import com.homekm.tag.dto.TagRequest;
import com.homekm.tag.dto.TagResponse;
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

    public List<TagResponse> list() {
        return tagRepository.findAll().stream().map(TagResponse::from).toList();
    }

    public List<TagResponse> autocomplete(String q) {
        return tagRepository.findByNameContaining(q).stream().map(TagResponse::from).toList();
    }

    @Transactional
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
}
