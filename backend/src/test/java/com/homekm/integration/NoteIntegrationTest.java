package com.homekm.integration;

import com.homekm.auth.dto.LoginResponse;
import com.homekm.auth.dto.RegisterRequest;
import com.homekm.note.dto.NoteDetail;
import com.homekm.note.dto.NoteRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class NoteIntegrationTest extends IntegrationTestBase {

    @Autowired TestRestTemplate rest;

    private String token;

    @BeforeEach
    void registerAndLogin() {
        String email = "notes_user_" + System.nanoTime() + "@test.com";
        ResponseEntity<LoginResponse> resp = rest.postForEntity("/api/auth/register",
                new RegisterRequest(email, "Notes1234", "Notes User"), LoginResponse.class);
        token = resp.getBody().token();
    }

    private HttpEntity<Object> auth(Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    @Test
    void createNote_returns201WithId() {
        var resp = rest.exchange("/api/notes", HttpMethod.POST,
                auth(new NoteRequest("My First Note", "Hello world", "custom", null, false, null, null)),
                NoteDetail.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody().id()).isPositive();
        assertThat(resp.getBody().title()).isEqualTo("My First Note");
        assertThat(resp.getBody().label()).isEqualTo("custom");
    }

    @Test
    void getNote_returns200WithBody() {
        var created = rest.exchange("/api/notes", HttpMethod.POST,
                auth(new NoteRequest("Readable Note", "Body text", "todo", null, false, null, null)),
                NoteDetail.class).getBody();

        var resp = rest.exchange("/api/notes/" + created.id(), HttpMethod.GET,
                auth(null), NoteDetail.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().body()).isEqualTo("Body text");
    }

    @Test
    void updateNote_changesTitle() {
        var created = rest.exchange("/api/notes", HttpMethod.POST,
                auth(new NoteRequest("Old Title", null, "custom", null, false, null, null)),
                NoteDetail.class).getBody();

        var resp = rest.exchange("/api/notes/" + created.id(), HttpMethod.PUT,
                auth(Map.of("title", "New Title")), NoteDetail.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().title()).isEqualTo("New Title");
    }

    @Test
    void deleteNote_returns204() {
        var created = rest.exchange("/api/notes", HttpMethod.POST,
                auth(new NoteRequest("Delete Me", null, "custom", null, false, null, null)),
                NoteDetail.class).getBody();

        var resp = rest.exchange("/api/notes/" + created.id(), HttpMethod.DELETE,
                auth(null), Void.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void deleteNote_secondTime_returns404() {
        var created = rest.exchange("/api/notes", HttpMethod.POST,
                auth(new NoteRequest("Gone Note", null, "custom", null, false, null, null)),
                NoteDetail.class).getBody();

        rest.exchange("/api/notes/" + created.id(), HttpMethod.DELETE, auth(null), Void.class);

        var resp = rest.exchange("/api/notes/" + created.id(), HttpMethod.GET,
                auth(null), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void unauthenticated_returns401() {
        ResponseEntity<String> resp = rest.getForEntity("/api/notes", String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void createNote_missingTitle_returns400() {
        var resp = rest.exchange("/api/notes", HttpMethod.POST,
                auth(Map.of("title", "", "label", "custom")), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
