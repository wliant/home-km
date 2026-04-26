package com.homekm.integration;

import com.homekm.admin.dto.CreateUserRequest;
import com.homekm.auth.dto.LoginRequest;
import com.homekm.auth.dto.LoginResponse;
import com.homekm.auth.dto.RegisterRequest;
import com.homekm.common.PageResponse;
import com.homekm.note.dto.NoteDetail;
import com.homekm.note.dto.NoteRequest;
import com.homekm.note.dto.NoteSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NotePinIntegrationTest extends IntegrationTestBase {

    @Autowired TestRestTemplate rest;

    private String adminToken;
    private String childToken;

    @BeforeEach
    void setup() {
        String suffix = String.valueOf(System.nanoTime());

        ResponseEntity<LoginResponse> adminResp = rest.postForEntity("/api/auth/register",
                new RegisterRequest("pin_admin_" + suffix + "@test.com", "Admin1234", "Admin"),
                LoginResponse.class);
        adminToken = adminResp.getBody().token();

        String childEmail = "pin_child_" + suffix + "@test.com";
        rest.exchange("/api/admin/users", HttpMethod.POST,
                auth(adminToken, new CreateUserRequest(childEmail, "Child1234", "Child", false, true)),
                Void.class);

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

    private NoteDetail createNote(String token, String title, boolean childSafe) {
        return rest.exchange("/api/notes", HttpMethod.POST,
                auth(token, new NoteRequest(title, null, "custom", null, childSafe)),
                NoteDetail.class).getBody();
    }

    private List<NoteSummary> listNotes(String token) {
        return rest.exchange("/api/notes?size=50", HttpMethod.GET, auth(token, null),
                new ParameterizedTypeReference<PageResponse<NoteSummary>>() {})
                .getBody().content();
    }

    @Test
    void pin_returns200WithPinnedAtSet() {
        NoteDetail note = createNote(adminToken, "To Pin", false);

        ResponseEntity<NoteDetail> resp = rest.exchange(
                "/api/notes/" + note.id() + "/pin", HttpMethod.POST, auth(adminToken, null),
                NoteDetail.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().pinnedAt()).isNotNull();
    }

    @Test
    void unpin_clearsPinnedAt() {
        NoteDetail note = createNote(adminToken, "Pin then unpin", false);
        rest.exchange("/api/notes/" + note.id() + "/pin", HttpMethod.POST,
                auth(adminToken, null), NoteDetail.class);

        ResponseEntity<NoteDetail> resp = rest.exchange(
                "/api/notes/" + note.id() + "/pin", HttpMethod.DELETE, auth(adminToken, null),
                NoteDetail.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().pinnedAt()).isNull();
    }

    @Test
    void pin_isIdempotent_keepsOriginalTimestamp() {
        NoteDetail note = createNote(adminToken, "Idempotent", false);

        NoteDetail first = rest.exchange("/api/notes/" + note.id() + "/pin", HttpMethod.POST,
                auth(adminToken, null), NoteDetail.class).getBody();
        NoteDetail second = rest.exchange("/api/notes/" + note.id() + "/pin", HttpMethod.POST,
                auth(adminToken, null), NoteDetail.class).getBody();

        assertThat(first.pinnedAt()).isNotNull();
        assertThat(second.pinnedAt()).isEqualTo(first.pinnedAt());
    }

    @Test
    void pin_doesNotChangeUpdatedAt() {
        NoteDetail created = createNote(adminToken, "Stable updatedAt", false);
        NoteDetail beforePin = rest.exchange("/api/notes/" + created.id(), HttpMethod.GET,
                auth(adminToken, null), NoteDetail.class).getBody();

        NoteDetail pinned = rest.exchange("/api/notes/" + created.id() + "/pin", HttpMethod.POST,
                auth(adminToken, null), NoteDetail.class).getBody();

        assertThat(pinned.updatedAt()).isEqualTo(beforePin.updatedAt());
    }

    @Test
    void list_putsPinnedNotesFirst() {
        NoteDetail a = createNote(adminToken, "A first", false);
        NoteDetail b = createNote(adminToken, "B second", false);
        NoteDetail c = createNote(adminToken, "C third", false);

        rest.exchange("/api/notes/" + b.id() + "/pin", HttpMethod.POST,
                auth(adminToken, null), NoteDetail.class);

        List<NoteSummary> notes = listNotes(adminToken);
        List<Long> ids = notes.stream().map(NoteSummary::id).toList();

        // Without the pin, default order would be C, B, A (by updatedAt desc).
        // Pinning B should move it to the front: B, C, A.
        assertThat(ids.indexOf(b.id())).isEqualTo(0);
        assertThat(ids.indexOf(c.id())).isLessThan(ids.indexOf(a.id()));
    }

    @Test
    void pin_nonExistentNote_returns404() {
        ResponseEntity<String> resp = rest.exchange(
                "/api/notes/999999/pin", HttpMethod.POST, auth(adminToken, null), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void child_cannotPinAdultOnlyNote_returns404() {
        NoteDetail note = createNote(adminToken, "Adult only", false);

        ResponseEntity<String> resp = rest.exchange(
                "/api/notes/" + note.id() + "/pin", HttpMethod.POST, auth(childToken, null),
                String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void child_cannotPinAnotherUsersChildSafeNote_returns403() {
        NoteDetail note = createNote(adminToken, "Safe but not childs", true);

        ResponseEntity<String> resp = rest.exchange(
                "/api/notes/" + note.id() + "/pin", HttpMethod.POST, auth(childToken, null),
                String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void child_canPinOwnNote() {
        NoteDetail note = createNote(childToken, "My note", false);

        ResponseEntity<NoteDetail> resp = rest.exchange(
                "/api/notes/" + note.id() + "/pin", HttpMethod.POST, auth(childToken, null),
                NoteDetail.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().pinnedAt()).isNotNull();
    }
}
