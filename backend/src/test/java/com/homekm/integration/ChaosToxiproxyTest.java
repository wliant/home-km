package com.homekm.integration;

import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import eu.rekawek.toxiproxy.model.ToxicDirection;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.ToxiproxyContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Foundation chaos / fault-injection test. Demonstrates Toxiproxy can sit
 * between the JVM and PostgreSQL, inject latency, and recover. Future tests
 * (per gaps/reliability/circuit-breaker.md, retry-backoff.md) build on this
 * to exercise MinIO and DB resilience patterns end-to-end.
 *
 * <p>Not extends {@link IntegrationTestBase} — does not need the full Spring
 * context, just raw JDBC.
 */
@Testcontainers
class ChaosToxiproxyTest {

    private static final Network NET = Network.newNetwork();

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("pgvector/pgvector:pg15")
                    .withDatabaseName("chaos")
                    .withUsername("homekm")
                    .withPassword("homekm")
                    .withNetwork(NET)
                    .withNetworkAliases("postgres");

    @Container
    static final ToxiproxyContainer TOXI =
            new ToxiproxyContainer("ghcr.io/shopify/toxiproxy:2.5.0")
                    .withNetwork(NET);

    static Proxy dbProxy;
    static String proxiedJdbcUrl;

    @BeforeAll
    static void setUpProxy() throws Exception {
        ToxiproxyClient client = new ToxiproxyClient(TOXI.getHost(), TOXI.getControlPort());
        dbProxy = client.createProxy("postgres", "0.0.0.0:8666", "postgres:5432");
        Integer mappedPort = TOXI.getMappedPort(8666);
        proxiedJdbcUrl = "jdbc:postgresql://" + TOXI.getHost() + ":" + mappedPort + "/chaos";
    }

    @AfterAll
    static void tearDownProxy() throws Exception {
        if (dbProxy != null) dbProxy.delete();
    }

    @Test
    void cleanQuery_throughProxy_returnsImmediately() throws Exception {
        try (Connection c = DriverManager.getConnection(proxiedJdbcUrl, "homekm", "homekm")) {
            Instant start = Instant.now();
            try (Statement s = c.createStatement(); ResultSet r = s.executeQuery("SELECT 1")) {
                assertThat(r.next()).isTrue();
                assertThat(r.getInt(1)).isEqualTo(1);
            }
            Duration elapsed = Duration.between(start, Instant.now());
            assertThat(elapsed).isLessThan(Duration.ofSeconds(2));
        }
    }

    @Test
    void latencyInjection_makesQueriesSlow_thenRecovers() throws Exception {
        // 2-second downstream latency on every byte from postgres → app.
        dbProxy.toxics().latency("downstream-latency", ToxicDirection.DOWNSTREAM, 2_000);

        try (Connection c = DriverManager.getConnection(proxiedJdbcUrl, "homekm", "homekm")) {
            Instant start = Instant.now();
            try (Statement s = c.createStatement(); ResultSet r = s.executeQuery("SELECT 1")) {
                assertThat(r.next()).isTrue();
            }
            Duration elapsed = Duration.between(start, Instant.now());
            assertThat(elapsed)
                    .as("Latency toxic should add at least 2s to the query")
                    .isGreaterThan(Duration.ofSeconds(2));
        } finally {
            dbProxy.toxics().get("downstream-latency").remove();
        }

        // After removing the toxic, queries are fast again.
        try (Connection c = DriverManager.getConnection(proxiedJdbcUrl, "homekm", "homekm");
             Statement s = c.createStatement();
             ResultSet r = s.executeQuery("SELECT 1")) {
            assertThat(r.next()).isTrue();
        }
    }

    @Test
    void connectionStarvation_failsConnectAttempt() throws Exception {
        // Zero-bandwidth toxic blocks all bytes downstream — postgres cannot
        // complete the startup handshake, so DriverManager.getConnection itself
        // raises PSQLException after the JDBC connection timeout.
        dbProxy.toxics().bandwidth("starve", ToxicDirection.DOWNSTREAM, 0);

        try {
            org.junit.jupiter.api.Assertions.assertThrows(SQLException.class, () -> {
                java.util.Properties props = new java.util.Properties();
                props.put("user", "homekm");
                props.put("password", "homekm");
                props.put("connectTimeout", "2"); // seconds
                DriverManager.getConnection(proxiedJdbcUrl, props).close();
            });
        } finally {
            dbProxy.toxics().get("starve").remove();
        }
    }
}
