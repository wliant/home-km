package com.homekm.integration;

import com.homekm.auth.dto.LoginRequest;
import com.homekm.auth.dto.LoginResponse;
import com.homekm.auth.dto.RegisterRequest;
import com.homekm.auth.dto.UpdateMeRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import static org.assertj.core.api.Assertions.assertThat;

class AuthIntegrationTest extends IntegrationTestBase {

    @Autowired TestRestTemplate rest;

    @Test
    void register_firstUser_becomesAdmin() {
        RegisterRequest req = new RegisterRequest("admin@test.com", "Admin1234", "Admin User");
        ResponseEntity<LoginResponse> resp = rest.postForEntity("/api/auth/register", req, LoginResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().token()).isNotBlank();
        assertThat(resp.getBody().user().isAdmin()).isTrue();
        assertThat(resp.getBody().user().email()).isEqualTo("admin@test.com");
    }

    @Test
    void register_duplicateEmail_returns409() {
        RegisterRequest req = new RegisterRequest("dup@test.com", "Admin1234", "User");
        rest.postForEntity("/api/auth/register", req, Void.class);
        ResponseEntity<String> resp = rest.postForEntity("/api/auth/register", req, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resp.getBody()).contains("EMAIL_ALREADY_EXISTS");
    }

    @Test
    void login_validCredentials_returnsToken() {
        rest.postForEntity("/api/auth/register",
                new RegisterRequest("login_user@test.com", "Pass1234", "Login User"), Void.class);

        ResponseEntity<LoginResponse> resp = rest.postForEntity("/api/auth/login",
                new LoginRequest("login_user@test.com", "Pass1234"), LoginResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().token()).isNotBlank();
    }

    @Test
    void login_wrongPassword_returns401() {
        rest.postForEntity("/api/auth/register",
                new RegisterRequest("wrong_pw@test.com", "Correct1", "User"), Void.class);

        ResponseEntity<String> resp = rest.postForEntity("/api/auth/login",
                new LoginRequest("wrong_pw@test.com", "WrongPwd1"), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void login_unknownEmail_returns401() {
        ResponseEntity<String> resp = rest.postForEntity("/api/auth/login",
                new LoginRequest("nobody@test.com", "Whatever1"), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void getMe_withValidToken_returnsProfile() {
        ResponseEntity<LoginResponse> regResp = rest.postForEntity("/api/auth/register",
                new RegisterRequest("me_user@test.com", "Me1234567", "Me User"), LoginResponse.class);
        String token = regResp.getBody().token();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<String> resp = rest.exchange(
                "/api/auth/me", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("me_user@test.com");
    }

    @Test
    void getMe_withoutToken_returns401() {
        ResponseEntity<String> resp = rest.getForEntity("/api/auth/me", String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void updateMe_changesDisplayName() {
        ResponseEntity<LoginResponse> regResp = rest.postForEntity("/api/auth/register",
                new RegisterRequest("update_me@test.com", "Update1234", "Old Name"), LoginResponse.class);
        String token = regResp.getBody().token();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> resp = rest.exchange(
                "/api/auth/me", HttpMethod.PUT,
                new HttpEntity<>(new UpdateMeRequest("New Name", null, null), headers),
                String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("New Name");
    }
}
