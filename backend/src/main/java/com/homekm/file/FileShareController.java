package com.homekm.file;

import com.homekm.auth.UserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
public class FileShareController {

    private final FileShareService shareService;

    public FileShareController(FileShareService shareService) {
        this.shareService = shareService;
    }

    @GetMapping("/api/files/{fileId}/share-links")
    public ResponseEntity<List<ShareLinkResponse>> list(@PathVariable Long fileId,
                                                         @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) throw new AccessDeniedException("login required");
        return ResponseEntity.ok(shareService.list(fileId).stream()
                .map(ShareLinkResponse::from).toList());
    }

    @PostMapping("/api/files/{fileId}/share-links")
    public ResponseEntity<IssuedShareLinkResponse> create(@PathVariable Long fileId,
                                                            @Valid @RequestBody CreateShareLinkRequest req,
                                                            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) throw new AccessDeniedException("login required");
        var issued = shareService.create(fileId, req.ttlHours(), req.password(), req.maxDownloads(), principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(IssuedShareLinkResponse.from(issued));
    }

    @DeleteMapping("/api/files/share-links/{linkId}")
    public ResponseEntity<Void> revoke(@PathVariable Long linkId,
                                        @AuthenticationPrincipal UserPrincipal principal) {
        shareService.revoke(linkId, principal);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/share/{token}")
    public ResponseEntity<FileShareService.ResolvedShare> resolve(@PathVariable String token,
                                                                    @RequestBody(required = false) ResolveRequest req) throws Exception {
        return ResponseEntity.ok(shareService.resolve(token, req != null ? req.password() : null));
    }

    public record CreateShareLinkRequest(
            @Min(1) Integer ttlHours,
            @Size(min = 4, max = 80) String password,
            @Min(1) Integer maxDownloads
    ) {}

    public record ResolveRequest(String password) {}

    public record ShareLinkResponse(Long id, Long fileId, Instant expiresAt, Integer maxDownloads,
                                      int downloadCount, boolean revoked, boolean expired, boolean exhausted,
                                      boolean passwordProtected, Instant createdAt) {
        public static ShareLinkResponse from(FileShareLink l) {
            return new ShareLinkResponse(l.getId(), l.getFileId(), l.getExpiresAt(),
                    l.getMaxDownloads(), l.getDownloadCount(), l.isRevoked(), l.isExpired(), l.isExhausted(),
                    l.getPasswordHash() != null, l.getCreatedAt());
        }
    }

    public record IssuedShareLinkResponse(ShareLinkResponse link, String token) {
        public static IssuedShareLinkResponse from(FileShareService.IssuedShareLink i) {
            return new IssuedShareLinkResponse(ShareLinkResponse.from(i.link()), i.token());
        }
    }
}
