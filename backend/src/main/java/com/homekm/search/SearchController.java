package com.homekm.search;

import com.homekm.auth.UserPrincipal;
import com.homekm.common.PageResponse;
import com.homekm.search.dto.SearchResult;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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
            @RequestParam(required = false) List<Long> tagIds,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(
                searchService.search(q, types, folderId, tagIds, page, Math.min(size, 50), principal));
    }
}
