package com.homekm.search;

import com.homekm.auth.UserPrincipal;
import com.homekm.common.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/saved-searches")
public class SavedSearchController {

    private final SavedSearchRepository repo;

    public SavedSearchController(SavedSearchRepository repo) {
        this.repo = repo;
    }

    public record SavedSearchResponse(Long id, String name, String query, Instant createdAt) {
        static SavedSearchResponse from(SavedSearch s) {
            return new SavedSearchResponse(s.getId(), s.getName(), s.getQuery(), s.getCreatedAt());
        }
    }

    public record SavedSearchRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank String query
    ) {}

    @GetMapping
    public ResponseEntity<List<SavedSearchResponse>> list(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(
                repo.findByUserIdOrderByCreatedAtDesc(principal.getId()).stream()
                        .map(SavedSearchResponse::from)
                        .toList());
    }

    @PostMapping
    public ResponseEntity<SavedSearchResponse> create(@Valid @RequestBody SavedSearchRequest req,
                                                       @AuthenticationPrincipal UserPrincipal principal) {
        if (repo.existsByUserIdAndName(principal.getId(), req.name())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "DUPLICATE_NAME");
        }
        SavedSearch s = new SavedSearch();
        s.setUserId(principal.getId());
        s.setName(req.name());
        s.setQuery(req.query());
        return ResponseEntity.status(HttpStatus.CREATED).body(SavedSearchResponse.from(repo.save(s)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                        @AuthenticationPrincipal UserPrincipal principal) {
        SavedSearch s = repo.findByIdAndUserId(id, principal.getId())
                .orElseThrow(() -> new EntityNotFoundException("Saved search not found"));
        repo.delete(s);
        return ResponseEntity.noContent().build();
    }
}
