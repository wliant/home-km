package com.homekm.tag;

import com.homekm.auth.UserPrincipal;
import com.homekm.tag.dto.TagRequest;
import com.homekm.tag.dto.TagResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tags")
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping
    public ResponseEntity<List<TagResponse>> list(@RequestParam(required = false) String q) {
        if (q != null && !q.isBlank()) return ResponseEntity.ok(tagService.autocomplete(q));
        return ResponseEntity.ok(tagService.list());
    }

    @PostMapping
    public ResponseEntity<TagResponse> create(@Valid @RequestBody TagRequest req,
                                               @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tagService.create(req, principal));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TagResponse> update(@PathVariable Long id,
                                               @Valid @RequestBody TagRequest req,
                                               @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(tagService.update(id, req, principal));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                        @AuthenticationPrincipal UserPrincipal principal) {
        tagService.delete(id, principal);
        return ResponseEntity.noContent().build();
    }

    // Tag attachment endpoints for notes
    @GetMapping("/notes/{entityId}/tags")
    public ResponseEntity<List<TagResponse>> getNoteTags(@PathVariable Long entityId) {
        return ResponseEntity.ok(tagService.getTagsForEntity("note", entityId));
    }

    @PostMapping("/notes/{entityId}/tags")
    public ResponseEntity<List<TagResponse>> attachToNote(@PathVariable Long entityId,
                                                           @RequestBody Map<String, List<Long>> body,
                                                           @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(tagService.attachTags("note", entityId, body.get("tagIds"), principal));
    }

    @DeleteMapping("/notes/{entityId}/tags/{tagId}")
    public ResponseEntity<Void> detachFromNote(@PathVariable Long entityId, @PathVariable Long tagId,
                                                @AuthenticationPrincipal UserPrincipal principal) {
        tagService.detachTag("note", entityId, tagId, principal);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/files/{entityId}/tags")
    public ResponseEntity<List<TagResponse>> getFileTags(@PathVariable Long entityId) {
        return ResponseEntity.ok(tagService.getTagsForEntity("file", entityId));
    }

    @PostMapping("/files/{entityId}/tags")
    public ResponseEntity<List<TagResponse>> attachToFile(@PathVariable Long entityId,
                                                           @RequestBody Map<String, List<Long>> body,
                                                           @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(tagService.attachTags("file", entityId, body.get("tagIds"), principal));
    }

    @DeleteMapping("/files/{entityId}/tags/{tagId}")
    public ResponseEntity<Void> detachFromFile(@PathVariable Long entityId, @PathVariable Long tagId,
                                                @AuthenticationPrincipal UserPrincipal principal) {
        tagService.detachTag("file", entityId, tagId, principal);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/folders/{entityId}/tags")
    public ResponseEntity<List<TagResponse>> getFolderTags(@PathVariable Long entityId) {
        return ResponseEntity.ok(tagService.getTagsForEntity("folder", entityId));
    }

    @PostMapping("/folders/{entityId}/tags")
    public ResponseEntity<List<TagResponse>> attachToFolder(@PathVariable Long entityId,
                                                             @RequestBody Map<String, List<Long>> body,
                                                             @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(tagService.attachTags("folder", entityId, body.get("tagIds"), principal));
    }

    @DeleteMapping("/folders/{entityId}/tags/{tagId}")
    public ResponseEntity<Void> detachFromFolder(@PathVariable Long entityId, @PathVariable Long tagId,
                                                  @AuthenticationPrincipal UserPrincipal principal) {
        tagService.detachTag("folder", entityId, tagId, principal);
        return ResponseEntity.noContent().build();
    }
}
