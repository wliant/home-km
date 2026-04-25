package com.homekm.folder;

import com.homekm.auth.UserPrincipal;
import com.homekm.folder.dto.FolderRequest;
import com.homekm.folder.dto.FolderResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/folders")
public class FolderController {

    private final FolderService folderService;

    public FolderController(FolderService folderService) {
        this.folderService = folderService;
    }

    @GetMapping
    public ResponseEntity<List<FolderResponse>> getTree(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(folderService.getTree(principal));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FolderResponse> getById(@PathVariable Long id,
                                                   @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(folderService.getById(id, principal));
    }

    @PostMapping
    public ResponseEntity<FolderResponse> create(@Valid @RequestBody FolderRequest req,
                                                  @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(folderService.create(req, principal));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FolderResponse> update(@PathVariable Long id,
                                                  @Valid @RequestBody FolderRequest req,
                                                  @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(folderService.update(id, req, principal));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                        @RequestParam(defaultValue = "false") boolean force,
                                        @AuthenticationPrincipal UserPrincipal principal) {
        folderService.delete(id, force, principal);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/child-safe")
    public ResponseEntity<FolderResponse> setChildSafe(@PathVariable Long id,
                                                        @RequestBody Map<String, Boolean> body,
                                                        @AuthenticationPrincipal UserPrincipal principal) {
        boolean childSafe = Boolean.TRUE.equals(body.get("isChildSafe"));
        return ResponseEntity.ok(folderService.setChildSafe(id, childSafe, principal));
    }
}
