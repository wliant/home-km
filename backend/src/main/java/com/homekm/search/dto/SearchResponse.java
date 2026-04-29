package com.homekm.search.dto;

import com.homekm.common.PageResponse;

import java.util.List;

/**
 * Wraps {@link PageResponse} with an optional "did you mean" suggestion.
 * Suggestion is non-null only when the query returned zero results AND the
 * trigram similarity engine found a near-match.
 */
public record SearchResponse(
        List<SearchResult> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        String suggestion
) {
    public static SearchResponse of(PageResponse<SearchResult> page, String suggestion) {
        return new SearchResponse(
                page.content(),
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages(),
                page.first(),
                page.last(),
                suggestion
        );
    }
}
