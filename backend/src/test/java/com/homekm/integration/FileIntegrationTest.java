package com.homekm.integration;

import com.homekm.admin.dto.CreateUserRequest;
import com.homekm.auth.User;
import com.homekm.auth.UserRepository;
import com.homekm.auth.dto.LoginRequest;
import com.homekm.auth.dto.LoginResponse;
import com.homekm.auth.dto.RegisterRequest;
import com.homekm.file.StoredFile;
import com.homekm.file.StoredFileRepository;
import com.homekm.file.dto.FileResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FileIntegrationTest extends IntegrationTestBase {

    @Autowired TestRestTemplate rest;
    @Autowired StoredFileRepository fileRepository;
    @Autowired UserRepository userRepository;

    private String ownerToken;
    private String ownerEmail;

    @BeforeEach
    void registerAndLogin() {
        ownerEmail = "files_owner_" + System.nanoTime() + "@test.com";
        ResponseEntity<LoginResponse> resp = rest.postForEntity("/api/auth/register",
                new RegisterRequest(ownerEmail, "Files1234", "Files Owner"), LoginResponse.class);
        ownerToken = resp.getBody().token();
    }

    private HttpEntity<Object> auth(String token, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    @Transactional
    long seedFile(String ownerEmail, String filename, boolean childSafe) {
        User owner = userRepository.findByEmail(ownerEmail).orElseThrow();
        StoredFile f = new StoredFile();
        f.setOwner(owner);
        f.setFilename(filename);
        f.setMimeType("application/pdf");
        f.setSizeBytes(1024L);
        f.setMinioKey("test/" + System.nanoTime() + "/" + filename);
        f.setChildSafe(childSafe);
        return fileRepository.save(f).getId();
    }

    @Test
    void renameFile_returns200WithNewFilename() {
        long id = seedFile(ownerEmail, "IMG_3829.JPG", false);

        var resp = rest.exchange("/api/files/" + id, HttpMethod.PUT,
                auth(ownerToken, Map.of("filename", "Kitchen tap warranty.jpg")),
                FileResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().filename()).isEqualTo("Kitchen tap warranty.jpg");
        assertThat(fileRepository.findById(id).orElseThrow().getFilename())
                .isEqualTo("Kitchen tap warranty.jpg");
    }

    @Test
    void renameFile_pathTraversalIsSanitizedToBasename() {
        long id = seedFile(ownerEmail, "report.pdf", false);

        var resp = rest.exchange("/api/files/" + id, HttpMethod.PUT,
                auth(ownerToken, Map.of("filename", "../etc/passwd")),
                FileResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().filename()).isEqualTo("passwd");
    }

    @Test
    void renameFile_blankFilenameReturns400() {
        long id = seedFile(ownerEmail, "report.pdf", false);

        var resp = rest.exchange("/api/files/" + id, HttpMethod.PUT,
                auth(ownerToken, Map.of("filename", "   ")), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void renameFile_emptyStringReturns400() {
        long id = seedFile(ownerEmail, "report.pdf", false);

        var resp = rest.exchange("/api/files/" + id, HttpMethod.PUT,
                auth(ownerToken, Map.of("filename", "")), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void renameFile_childCannotRenameOthersFile() {
        // Owner seeds a child-safe file the child can see
        long id = seedFile(ownerEmail, "shared.pdf", true);

        // Owner (admin, since first registered user is admin) creates a child account
        String suffix = String.valueOf(System.nanoTime());
        String childEmail = "files_child_" + suffix + "@test.com";
        rest.exchange("/api/admin/users", HttpMethod.POST,
                auth(ownerToken, new CreateUserRequest(childEmail, "Child1234", "Child", false, true)),
                Void.class);

        String childToken = rest.postForEntity("/api/auth/login",
                new LoginRequest(childEmail, "Child1234"), LoginResponse.class).getBody().token();

        var resp = rest.exchange("/api/files/" + id, HttpMethod.PUT,
                auth(childToken, Map.of("filename", "stolen.pdf")), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resp.getBody()).contains("CHILD_ACCOUNT_READ_ONLY");
    }

    @Test
    void renameFile_unauthenticatedReturns401() {
        long id = seedFile(ownerEmail, "report.pdf", false);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        var resp = rest.exchange("/api/files/" + id, HttpMethod.PUT,
                new HttpEntity<>(Map.of("filename", "x.pdf"), headers), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
