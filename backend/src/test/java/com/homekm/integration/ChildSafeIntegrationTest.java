package com.homekm.integration;

import com.homekm.admin.dto.CreateUserRequest;
import com.homekm.auth.dto.LoginRequest;
import com.homekm.auth.dto.LoginResponse;
import com.homekm.auth.dto.RegisterRequest;
import com.homekm.common.PageResponse;
import com.homekm.note.dto.NoteDetail;
import com.homekm.note.dto.NoteSummary;
import com.homekm.note.dto.NoteRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import static org.assertj.core.api.Assertions.assertThat;

class ChildSafeIntegrationTest extends IntegrationTestBase {

    @Autowired TestRestTemplate rest;

    private String adminToken;
    private String childToken;

    @BeforeEach
    void setup() {
        String suffix = String.valueOf(System.nanoTime());

        // First user becomes admin
        ResponseEntity<LoginResponse> adminResp = rest.postForEntity("/api/auth/register",
                new RegisterRequest("cs_admin_" + suffix + "@test.com", "Admin1234", "Admin"), LoginResponse.class);
        adminToken = adminResp.getBody().token();

        // Admin creates a child account
        String childEmail = "cs_child_" + suffix + "@test.com";
        HttpHeaders adminHeaders = new HttpHeaders();
        adminHeaders.setBearerAuth(adminToken);
        adminHeaders.setContentType(MediaType.APPLICATION_JSON);
        rest.exchange("/api/admin/users", HttpMethod.POST,
                new HttpEntity<>(new CreateUserRequest(childEmail, "Child1234", "Child User", false, true), adminHeaders),
                Void.class);

        // Child logs in
        ResponseEntity<LoginResponse> childResp = rest.postForEntity("/api/auth/login",
                new LoginRequest(childEmail, "Child1234"), LoginResponse.class);
        childToken = childResp.getBody().token();
    }

    private HttpEntity<Object> auth(String token, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    @Test
    void childCannotSeeUnsafeNote() {
        // Admin creates an unsafe note
        NoteDetail note = rest.exchange("/api/notes", HttpMethod.POST,
                auth(adminToken, new NoteRequest("Secret Note", "admin only", "custom", null, false, null, null)),
                NoteDetail.class).getBody();

        // Child tries to fetch it → 404
        ResponseEntity<String> resp = rest.exchange(
                "/api/notes/" + note.id(), HttpMethod.GET, auth(childToken, null), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void childCanSeeChildSafeNote() {
        // Admin creates a child-safe note
        NoteDetail note = rest.exchange("/api/notes", HttpMethod.POST,
                auth(adminToken, new NoteRequest("Safe Note", "visible to all", "custom", null, true, null, null)),
                NoteDetail.class).getBody();

        // Child can fetch it
        ResponseEntity<NoteDetail> resp = rest.exchange(
                "/api/notes/" + note.id(), HttpMethod.GET, auth(childToken, null), NoteDetail.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().title()).isEqualTo("Safe Note");
    }

    @Test
    void childNoteListExcludesUnsafeNotes() {
        // Admin creates one safe and one unsafe note
        rest.exchange("/api/notes", HttpMethod.POST,
                auth(adminToken, new NoteRequest("Visible Note", null, "custom", null, true, null, null)), NoteDetail.class);
        rest.exchange("/api/notes", HttpMethod.POST,
                auth(adminToken, new NoteRequest("Hidden Note", null, "custom", null, false, null, null)), NoteDetail.class);

        // Child lists notes — should only see child-safe ones
        ResponseEntity<PageResponse<NoteSummary>> resp = rest.exchange(
                "/api/notes", HttpMethod.GET, auth(childToken, null),
                new ParameterizedTypeReference<>() {});

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        boolean hiddenFound = resp.getBody().content().stream()
                .anyMatch(n -> n.title().equals("Hidden Note"));
        assertThat(hiddenFound).isFalse();
    }

    @Test
    void childCannotDeleteNote() {
        // Admin creates a child-safe note
        NoteDetail note = rest.exchange("/api/notes", HttpMethod.POST,
                auth(adminToken, new NoteRequest("Safe Deletable Note", null, "custom", null, true, null, null)),
                NoteDetail.class).getBody();

        // Child tries to delete it → 403
        ResponseEntity<String> resp = rest.exchange(
                "/api/notes/" + note.id(), HttpMethod.DELETE, auth(childToken, null), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
