package com.homekm.trash;

import com.homekm.auth.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TrashController {

    private final TrashService trashService;

    public TrashController(TrashService trashService) {
        this.trashService = trashService;
    }

    @GetMapping("/api/trash")
    public ResponseEntity<TrashResponse> getTrash(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(trashService.getTrash(principal));
    }
}
