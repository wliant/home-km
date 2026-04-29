package com.homekm.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Detects drift between the live OpenAPI spec served by the Spring app and the
 * checked-in baseline at {@code backend/openapi.yaml}. The frontend type
 * generator should consume the same baseline file.
 *
 * <p>To regenerate the baseline (after intentional API changes):
 * <pre>{@code
 *   UPDATE_OPENAPI_BASELINE=1 ./gradlew test --tests "*OpenApiContractTest"
 * }</pre>
 * Then commit the updated {@code backend/openapi.yaml}.
 */
class OpenApiContractTest extends IntegrationTestBase {

    private static final Path BASELINE = Path.of("openapi.yaml");

    @Autowired
    TestRestTemplate rest;

    @Test
    void apiSpec_matchesCommittedBaseline() throws Exception {
        ResponseEntity<String> resp = rest.getForEntity("/v3/api-docs.yaml", String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);

        String live = resp.getBody();
        assertThat(live)
                .as("OpenAPI YAML must be returned by /v3/api-docs.yaml")
                .isNotBlank();

        if ("1".equals(System.getenv("UPDATE_OPENAPI_BASELINE"))) {
            Files.writeString(BASELINE, live);
            return;
        }

        if (!Files.exists(BASELINE)) {
            throw new AssertionError(
                    "OpenAPI baseline missing at " + BASELINE.toAbsolutePath() + ". "
                            + "Generate it with: UPDATE_OPENAPI_BASELINE=1 ./gradlew test --tests \"*OpenApiContractTest\"");
        }

        String committed = Files.readString(BASELINE);
        assertThat(live)
                .as("OpenAPI spec drifted from %s. Run UPDATE_OPENAPI_BASELINE=1 to regenerate.",
                        BASELINE.toAbsolutePath())
                .isEqualTo(committed);
    }
}
