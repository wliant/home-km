package com.homekm.search;

import com.homekm.auth.UserPrincipal;
import com.homekm.common.PageResponse;
import com.homekm.common.Pagination;
import com.homekm.search.dto.SearchResult;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/search")
@Validated
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<SearchResult>> search(
            @RequestParam @NotBlank @Size(min = 1, max = 500) String q,
            @RequestParam(required = false) List<String> types,
            @RequestParam(required = false) Long folderId,
            @RequestParam(required = false, defaultValue = "true") boolean includeSubfolders,
            @RequestParam(required = false) List<Long> tagIds,
            @RequestParam(required = false) Long ownerId,
            @RequestParam(required = false) String mimePrefix,
            @RequestParam(required = false) Boolean hasReminder,
            @RequestParam(required = false) Boolean childSafe,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false, defaultValue = "false") boolean smart,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        SearchService.SearchOpts opts = new SearchService.SearchOpts(types, folderId, includeSubfolders,
                tagIds, ownerId, mimePrefix, hasReminder, childSafe, from, to, smart);
        // Search is more expensive than other lists; cap below the global ceiling.
        int searchSize = Math.min(Pagination.clampSize(size), 50);
        return ResponseEntity.ok(
                searchService.search(q, opts, Pagination.clampPage(page), searchSize, principal));
    }
}
