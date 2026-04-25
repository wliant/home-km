package com.homekm.integration;

import com.homekm.auth.dto.LoginResponse;
import com.homekm.auth.dto.RegisterRequest;
import com.homekm.common.PageResponse;
import com.homekm.note.dto.NoteDetail;
import com.homekm.note.dto.NoteRequest;
import com.homekm.search.dto.SearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import static org.assertj.core.api.Assertions.assertThat;

class SearchIntegrationTest extends IntegrationTestBase {

    @Autowired TestRestTemplate rest;

    private String token;

    @BeforeEach
    void setup() {
        String suffix = String.valueOf(System.nanoTime());
        ResponseEntity<LoginResponse> resp = rest.postForEntity("/api/auth/register",
                new RegisterRequest("search_user_" + suffix + "@test.com", "Search1234", "Searcher"),
                LoginResponse.class);
        token = resp.getBody().token();
    }

    private HttpEntity<Object> auth(Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    @Test
    void searchFindsNoteByTitle() {
        String unique = "xyzuniq" + System.nanoTime();
        rest.exchange("/api/notes", HttpMethod.POST,
                auth(new NoteRequest(unique + " recipe", "ingredients here", "recipe", null, false)),
                NoteDetail.class);

        ResponseEntity<PageResponse<SearchResult>> resp = rest.exchange(
                "/api/search?q=" + unique, HttpMethod.GET, auth(null),
                new ParameterizedTypeReference<>() {});

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().content()).isNotEmpty();
        assertThat(resp.getBody().content().get(0).title()).contains(unique);
    }

    @Test
    void searchFindsNoteByBody() {
        String unique = "bodyterm" + System.nanoTime();
        rest.exchange("/api/notes", HttpMethod.POST,
                auth(new NoteRequest("Generic Title", unique, "custom", null, false)),
                NoteDetail.class);

        ResponseEntity<PageResponse<SearchResult>> resp = rest.exchange(
                "/api/search?q=" + unique, HttpMethod.GET, auth(null),
                new ParameterizedTypeReference<>() {});

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().content()).isNotEmpty();
    }

    @Test
    void searchWithNoResults_returnsEmptyPage() {
        ResponseEntity<PageResponse<SearchResult>> resp = rest.exchange(
                "/api/search?q=zzznomatchterm999", HttpMethod.GET, auth(null),
                new ParameterizedTypeReference<>() {});

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().content()).isEmpty();
        assertThat(resp.getBody().totalElements()).isZero();
    }

    @Test
    void searchWithTypeFilter_onlyReturnsMatchingType() {
        String unique = "typefilter" + System.nanoTime();
        rest.exchange("/api/notes", HttpMethod.POST,
                auth(new NoteRequest(unique + " note", null, "custom", null, false)),
                NoteDetail.class);

        ResponseEntity<PageResponse<SearchResult>> notesOnly = rest.exchange(
                "/api/search?q=" + unique + "&types=note", HttpMethod.GET, auth(null),
                new ParameterizedTypeReference<>() {});

        assertThat(notesOnly.getStatusCode()).isEqualTo(HttpStatus.OK);
        notesOnly.getBody().content().forEach(r -> assertThat(r.type()).isEqualTo("note"));

        ResponseEntity<PageResponse<SearchResult>> filesOnly = rest.exchange(
                "/api/search?q=" + unique + "&types=file", HttpMethod.GET, auth(null),
                new ParameterizedTypeReference<>() {});

        assertThat(filesOnly.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(filesOnly.getBody().content()).isEmpty();
    }

    @Test
    void searchRequiresAuth() {
        ResponseEntity<String> resp = rest.getForEntity("/api/search?q=anything", String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
