package com.homekm.note;

import com.homekm.auth.UserPrincipal;
import com.homekm.common.PageResponse;
import com.homekm.common.Pagination;
import com.homekm.note.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class NoteController {

    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    @GetMapping("/api/notes")
    public ResponseEntity<PageResponse<NoteSummary>> list(
            @RequestParam(required = false) Long folderId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(noteService.list(folderId, Pagination.clampPage(page), Pagination.clampSize(size), principal));
    }

    @GetMapping("/api/folders/{folderId}/notes")
    public ResponseEntity<PageResponse<NoteSummary>> listByFolder(
            @PathVariable Long folderId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(noteService.list(folderId, Pagination.clampPage(page), Pagination.clampSize(size), principal));
    }

    @GetMapping("/api/notes/templates")
    public ResponseEntity<List<NoteSummary>> listTemplates(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(noteService.listTemplates(principal));
    }

    @PostMapping("/api/notes/from-template/{templateId}")
    public ResponseEntity<NoteDetail> createFromTemplate(@PathVariable Long templateId,
                                                          @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(noteService.createFromTemplate(templateId, principal));
    }

    @GetMapping("/api/notes/{id}/revisions")
    public ResponseEntity<List<NoteService.RevisionResponse>> listRevisions(
            @PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(noteService.listRevisions(id, principal));
    }

    @PostMapping("/api/notes/{id}/revisions/{revisionId}/restore")
    public ResponseEntity<NoteDetail> restoreRevision(@PathVariable Long id,
                                                       @PathVariable Long revisionId,
                                                       @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(noteService.restoreRevision(id, revisionId, principal));
    }

    @GetMapping("/api/notes/{id}")
    public ResponseEntity<NoteDetail> getById(@PathVariable Long id,
                                               @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(noteService.getById(id, principal));
    }

    @PostMapping("/api/notes")
    public ResponseEntity<NoteDetail> create(@Valid @RequestBody NoteRequest req,
                                              @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(noteService.create(req, principal));
    }

    @PutMapping("/api/notes/{id}")
    public ResponseEntity<NoteDetail> update(@PathVariable Long id,
                                              @Valid @RequestBody NoteRequest req,
                                              @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(noteService.update(id, req, principal));
    }

    @DeleteMapping("/api/notes/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                        @AuthenticationPrincipal UserPrincipal principal) {
        noteService.delete(id, principal);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/notes/{id}/restore")
    public ResponseEntity<Void> restore(@PathVariable Long id,
                                         @AuthenticationPrincipal UserPrincipal principal) {
        noteService.restore(id, principal);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/notes/{id}/pin")
    public ResponseEntity<NoteDetail> pin(@PathVariable Long id,
                                           @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(noteService.pin(id, principal));
    }

    @DeleteMapping("/api/notes/{id}/pin")
    public ResponseEntity<NoteDetail> unpin(@PathVariable Long id,
                                             @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(noteService.unpin(id, principal));
    }

    // Checklist items

    @GetMapping("/api/notes/{noteId}/checklist-items")
    public ResponseEntity<List<NoteDetail.ChecklistItemResponse>> listChecklist(
            @PathVariable Long noteId, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(noteService.listChecklistItems(noteId, principal));
    }

    @PostMapping("/api/notes/{noteId}/checklist-items")
    public ResponseEntity<NoteDetail.ChecklistItemResponse> addChecklistItem(
            @PathVariable Long noteId,
            @Valid @RequestBody ChecklistItemRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(noteService.addChecklistItem(noteId, req, principal));
    }

    @PutMapping("/api/notes/{noteId}/checklist-items/{itemId}")
    public ResponseEntity<NoteDetail.ChecklistItemResponse> updateChecklistItem(
            @PathVariable Long noteId, @PathVariable Long itemId,
            @Valid @RequestBody ChecklistItemRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(noteService.updateChecklistItem(noteId, itemId, req, principal));
    }

    @DeleteMapping("/api/notes/{noteId}/checklist-items/{itemId}")
    public ResponseEntity<Void> deleteChecklistItem(
            @PathVariable Long noteId, @PathVariable Long itemId,
            @AuthenticationPrincipal UserPrincipal principal) {
        noteService.deleteChecklistItem(noteId, itemId, principal);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/api/notes/{noteId}/checklist-items/reorder")
    public ResponseEntity<List<NoteDetail.ChecklistItemResponse>> reorderChecklistItems(
            @PathVariable Long noteId,
            @Valid @RequestBody ReorderRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(noteService.reorderChecklistItems(noteId, req, principal));
    }
}
