package dev.sentinel.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * End-to-end tests against a real PostgreSQL started by Testcontainers (needs Docker).
 * Each test uses its own account id, so tests never see each other's transactions.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class SentinelApiIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    // ── helpers ───────────────────────────────────────────────────

    private String login(String username, String password) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        return json.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private String service() throws Exception { return login("service", "service-test-pass"); }
    private String analyst() throws Exception { return login("analyst", "analyst-test-pass"); }
    private String admin() throws Exception { return login("admin", "admin-test-pass"); }

    private static String account() {
        return "ACC-" + UUID.randomUUID();
    }

    private static String body(String account, String amount, String country, String device, Instant at) {
        return """
                {"externalId":"EXT-%s","accountId":"%s","merchantId":"MER-1","amount":%s,
                 "currency":"ZAR","country":"%s","deviceId":"%s","occurredAt":"%s"}
                """.formatted(UUID.randomUUID(), account, amount, country, device, at);
    }

    private MvcResult ingest(String token, String key, String body) throws Exception {
        return mvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();
    }

    private JsonNode read(MvcResult result) throws Exception {
        return json.readTree(result.getResponse().getContentAsString());
    }

    // ── security ──────────────────────────────────────────────────

    @Test
    void requestsWithoutATokenAreRejected() throws Exception {
        mvc.perform(get("/api/v1/alerts")).andExpect(status().isUnauthorized());
    }

    @Test
    void wrongPasswordIsUnauthorized() throws Exception {
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"analyst\",\"password\":\"nope\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Unauthorized"));
    }

    @Test
    void anAnalystCannotIngestTransactions() throws Exception {
        MvcResult result = ingest(analyst(), UUID.randomUUID().toString(),
                body(account(), "100.00", "ZA", "DEV-1", Instant.now()));
        assertEquals(403, result.getResponse().getStatus());
    }

    @Test
    void aServiceAccountCannotReadAlerts() throws Exception {
        mvc.perform(get("/api/v1/alerts").header("Authorization", "Bearer " + service()))
                .andExpect(status().isForbidden());
    }

    @Test
    void onlyAdminsCanManageTheBlacklist() throws Exception {
        mvc.perform(get("/api/v1/blacklist").header("Authorization", "Bearer " + analyst()))
                .andExpect(status().isForbidden());
    }

    // ── ingestion and decisions ───────────────────────────────────

    @Test
    void aPlainPaymentIsApproved() throws Exception {
        MvcResult result = ingest(service(), UUID.randomUUID().toString(),
                body(account(), "100.00", "ZA", "DEV-1", Instant.now()));
        assertEquals(201, result.getResponse().getStatus());
        JsonNode tx = read(result);
        assertEquals("APPROVE", tx.get("decision").asText());
        assertEquals(0, tx.get("riskScore").asInt());
    }

    @Test
    void aLargePaymentGoesToReviewWithAReason() throws Exception {
        MvcResult result = ingest(service(), UUID.randomUUID().toString(),
                body(account(), "7500.00", "ZA", "DEV-1", Instant.now()));
        assertEquals(201, result.getResponse().getStatus());
        JsonNode tx = read(result);
        assertEquals("REVIEW", tx.get("decision").asText());
        assertEquals("HIGH_AMOUNT", tx.get("reasons").get(0).get("ruleId").asText());
    }

    @Test
    void theSixthPaymentInTenMinutesRecordsAVelocityHit() throws Exception {
        String acc = account();
        String token = service();
        Instant start = Instant.now().minusSeconds(120);
        JsonNode last = null;
        for (int i = 0; i < 6; i++) {
            MvcResult r = ingest(token, UUID.randomUUID().toString(),
                    body(acc, "50.00", "ZA", "DEV-1", start.plusSeconds(i * 10L)));
            assertEquals(201, r.getResponse().getStatus());
            last = read(r);
        }
        boolean hasVelocity = false;
        for (JsonNode reason : last.get("reasons")) {
            hasVelocity |= "VELOCITY".equals(reason.get("ruleId").asText());
        }
        assertTrue(hasVelocity, "expected a VELOCITY hit but got " + last);
    }

    @Test
    void aBlacklistedAccountIsDeclined() throws Exception {
        String acc = account();
        mvc.perform(post("/api/v1/blacklist")
                        .header("Authorization", "Bearer " + admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"kind\":\"ACCOUNT\",\"value\":\"%s\",\"reason\":\"test\"}".formatted(acc)))
                .andExpect(status().isCreated());

        MvcResult result = ingest(service(), UUID.randomUUID().toString(),
                body(acc, "100.00", "ZA", "DEV-1", Instant.now()));
        JsonNode tx = read(result);
        assertEquals("DECLINE", tx.get("decision").asText());
        assertEquals(100, tx.get("riskScore").asInt());
    }

    // ── idempotency ───────────────────────────────────────────────

    @Test
    void retryingWithTheSameKeyReturnsTheOriginalTransaction() throws Exception {
        String token = service();
        String key = UUID.randomUUID().toString();
        String payload = body(account(), "7500.00", "ZA", "DEV-1", Instant.now());

        MvcResult first = ingest(token, key, payload);
        MvcResult second = ingest(token, key, payload);

        assertEquals(201, first.getResponse().getStatus());
        assertEquals(200, second.getResponse().getStatus());
        assertEquals("true", second.getResponse().getHeader("Idempotent-Replay"));
        assertEquals(read(first).get("id"), read(second).get("id"));
    }

    @Test
    void reusingAKeyWithADifferentBodyIsAConflict() throws Exception {
        String token = service();
        String key = UUID.randomUUID().toString();
        String acc = account();
        Instant at = Instant.now();

        ingest(token, key, body(acc, "100.00", "ZA", "DEV-1", at));
        MvcResult second = ingest(token, key, body(acc, "200.00", "ZA", "DEV-1", at));

        assertEquals(409, second.getResponse().getStatus());
    }

    // ── validation ────────────────────────────────────────────────

    @Test
    void tooManyDecimalPlacesAreRejectedAsProblemJson() throws Exception {
        MvcResult result = ingest(service(), UUID.randomUUID().toString(),
                body(account(), "10.123", "ZA", "DEV-1", Instant.now()));
        assertEquals(400, result.getResponse().getStatus());
        JsonNode problem = read(result);
        assertTrue(problem.get("errors").has("amount"));
    }

    @Test
    void theIdempotencyKeyHeaderIsRequired() throws Exception {
        mvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + service())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(account(), "100.00", "ZA", "DEV-1", Instant.now())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad request"));
    }

    // ── analyst workflow ──────────────────────────────────────────

    @Test
    void anAnalystCanResolveAnAlertOnlyOnce() throws Exception {
        MvcResult created = ingest(service(), UUID.randomUUID().toString(),
                body(account(), "7500.00", "ZA", "DEV-1", Instant.now()));
        String txId = read(created).get("id").asText();

        String analystToken = analyst();
        MvcResult alerts = mvc.perform(get("/api/v1/alerts?status=OPEN&size=100")
                        .header("Authorization", "Bearer " + analystToken))
                .andExpect(status().isOk())
                .andReturn();
        long alertId = -1;
        for (JsonNode item : read(alerts).get("items")) {
            if (txId.equals(item.get("transaction").get("id").asText())) {
                alertId = item.get("id").asLong();
            }
        }
        assertTrue(alertId > 0, "the new alert should be in the open list");

        String resolution = "{\"status\":\"FALSE_POSITIVE\",\"note\":\"Customer confirmed the purchase\"}";
        mvc.perform(post("/api/v1/alerts/" + alertId + "/resolution")
                        .header("Authorization", "Bearer " + analystToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resolution))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FALSE_POSITIVE"))
                .andExpect(jsonPath("$.resolvedBy").value("analyst"));

        mvc.perform(post("/api/v1/alerts/" + alertId + "/resolution")
                        .header("Authorization", "Bearer " + analystToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resolution))
                .andExpect(status().isConflict());
    }

    @Test
    void everyResponseCarriesARequestId() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"));
    }
}
