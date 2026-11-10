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
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
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

    @Autowired
    MeterRegistry meters;

    @Autowired
    dev.sentinel.persistence.TransactionRepository transactions;

    // ── helpers ───────────────────────────────────────────────────

    private String login(String username, String password) throws Exception {
        return login(username, password, "127.0.0.1");
    }

    private String login(String username, String password, String clientIp) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/auth/login")
                        .with(clientIp(clientIp))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        return json.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private static RequestPostProcessor clientIp(String ip) {
        return request -> {
            request.setRemoteAddr(ip);
            return request;
        };
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
    void repeatedWrongPasswordsAreThrottled() throws Exception {
        String attempt = "{\"username\":\"throttle-victim\",\"password\":\"wrong\"}";
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(attempt))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(attempt))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.title").value("Too many attempts"));
    }

    @Test
    void accountThrottleSpansAddressesButDoesNotLockOutTheCorrectPassword() throws Exception {
        login("analyst", "analyst-test-pass", "192.0.2.250");
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/v1/auth/login").with(clientIp("192.0.2." + (i + 1)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"analyst\",\"password\":\"wrong\"}"))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/v1/auth/login").with(clientIp("192.0.2.6"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"analyst\",\"password\":\"wrong\"}"))
                .andExpect(status().isTooManyRequests());

        login("analyst", "analyst-test-pass", "192.0.2.7");

        mvc.perform(post("/api/v1/auth/login").with(clientIp("192.0.2.8"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"analyst\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
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
    void ingestionRecordsFraudHistoryLookupDuration() throws Exception {
        long before = meters.timer("sentinel.fraud.history.lookup").count();
        MvcResult result = ingest(service(), UUID.randomUUID().toString(),
                body(account(), "100.00", "ZA", "DEV-1", Instant.now()));

        assertEquals(201, result.getResponse().getStatus());
        assertTrue(meters.timer("sentinel.fraud.history.lookup").count() > before);
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
    void concurrentRequestsWithTheSameKeyCreateOneTransaction() throws Exception {
        String token = service();
        String key = UUID.randomUUID().toString();
        String payload = body(account(), "7500.00", "ZA", "DEV-1", Instant.now());
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<MvcResult> first = executor.submit(() -> {
                ready.countDown();
                if (!start.await(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Timed out waiting to start concurrent request");
                }
                return ingest(token, key, payload);
            });
            Future<MvcResult> second = executor.submit(() -> {
                ready.countDown();
                if (!start.await(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Timed out waiting to start concurrent request");
                }
                return ingest(token, key, payload);
            });

            assertTrue(ready.await(5, java.util.concurrent.TimeUnit.SECONDS));
            start.countDown();
            MvcResult firstResult = first.get();
            MvcResult secondResult = second.get();

            assertTrue(Set.of(200, 201).contains(firstResult.getResponse().getStatus()));
            assertTrue(Set.of(200, 201).contains(secondResult.getResponse().getStatus()));
            assertEquals(1, transactions.countByIdempotencyKey(key));
            assertEquals(read(firstResult).get("id"), read(secondResult).get("id"));
        } finally {
            executor.shutdownNow();
        }
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
    void currencyAndCountryMustMatchTheirDocumentedCodeFormats() throws Exception {
        String token = service();
        String valid = body(account(), "100.00", "ZA", "DEV-1", Instant.now());
        String invalidCurrency = valid.replace("\"currency\":\"ZAR\"", "\"currency\":\"zar\"");
        String invalidCountry = valid.replace("\"country\":\"ZA\"", "\"country\":\"Zaf\"");

        assertEquals(400, ingest(token, UUID.randomUUID().toString(), invalidCurrency).getResponse().getStatus());
        assertEquals(400, ingest(token, UUID.randomUUID().toString(), invalidCountry).getResponse().getStatus());
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
