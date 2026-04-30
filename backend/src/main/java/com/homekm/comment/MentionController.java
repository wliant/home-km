package com.homekm.comment;

import com.homekm.auth.UserPrincipal;
import com.homekm.comment.dto.MentionInboxResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Per-user mention inbox feeding the bell icon. Reads + writes only — comment
 * authorship lives in {@link CommentController}.
 */
@RestController
@RequestMapping("/api/me/mentions")
public class MentionController {

    private final CommentService service;

    public MentionController(CommentService service) {
        this.service = service;
    }

    public record UnreadCountResponse(long count) {}

    @GetMapping
    public ResponseEntity<List<MentionInboxResponse>> list(@AuthenticationPrincipal UserPrincipal principal) {
        require(principal);
        return ResponseEntity.ok(service.inbox(principal.getId()));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountResponse> unreadCount(@AuthenticationPrincipal UserPrincipal principal) {
        require(principal);
        return ResponseEntity.ok(new UnreadCountResponse(service.unreadCount(principal.getId())));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable Long id,
                                          @AuthenticationPrincipal UserPrincipal principal) {
        require(principal);
        service.markRead(id, principal.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllRead(@AuthenticationPrincipal UserPrincipal principal) {
        require(principal);
        service.markAllRead(principal.getId());
        return ResponseEntity.noContent().build();
    }

    private static void require(UserPrincipal principal) {
        if (principal == null) throw new AccessDeniedException("login required");
    }
}
