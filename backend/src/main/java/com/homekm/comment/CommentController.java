package com.homekm.comment;

import com.homekm.auth.UserPrincipal;
import com.homekm.comment.dto.CommentRequest;
import com.homekm.comment.dto.CommentResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Comments + mentions for notes ({@code itemType=note}) and files
 * ({@code itemType=file}). The frontend issues mentions explicitly via
 * {@code mentionedUserIds}/{@code mentionedGroupIds} rather than parsing
 * {@code @username} text — keeps the contract simple and dodges fragile
 * name-resolution rules.
 */
@RestController
@RequestMapping("/api/comments")
public class CommentController {

    private final CommentService service;

    public CommentController(CommentService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<CommentResponse>> list(
            @RequestParam("itemType") Comment.ItemType itemType,
            @RequestParam("itemId") Long itemId,
            @AuthenticationPrincipal UserPrincipal principal) {
        require(principal);
        return ResponseEntity.ok(service.list(itemType, itemId, principal));
    }

    @PostMapping
    public ResponseEntity<CommentResponse> create(
            @RequestParam("itemType") Comment.ItemType itemType,
            @RequestParam("itemId") Long itemId,
            @Valid @RequestBody CommentRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        require(principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(itemType, itemId, req, principal));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommentResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CommentRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        require(principal);
        return ResponseEntity.ok(service.update(id, req, principal));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                        @AuthenticationPrincipal UserPrincipal principal) {
        require(principal);
        service.delete(id, principal);
        return ResponseEntity.noContent().build();
    }

    private static void require(UserPrincipal principal) {
        if (principal == null) throw new AccessDeniedException("login required");
    }
}
