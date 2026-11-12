package dev.sentinel.api;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Proves the demo data really produces every kind of rule hit, so a live demo shows what the README promises. */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@TestPropertySource(properties = "sentinel.demo.seed=true")
class DemoSeedIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16");

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    private String analystToken() throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"analyst\",\"password\":\"analyst-test-pass\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return json.readTree(result.getResponse().getContentAsString()).get("accessToken").asString();
    }

    @Test
    void seededDataShowsEveryRuleAndLeavesAlertsToReview() throws Exception {
        String auth = "Bearer " + analystToken();

        MvcResult transactions = mvc.perform(get("/api/v1/transactions?size=100").header("Authorization", auth))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode items = json.readTree(transactions.getResponse().getContentAsString()).get("items");
        assertTrue(items.size() >= 20, "expected about 24 seeded transactions but got " + items.size());

        Set<String> rulesSeen = new HashSet<>();
        for (JsonNode tx : items) {
            for (JsonNode reason : tx.get("reasons")) {
                rulesSeen.add(reason.get("ruleId").asString());
            }
        }
        assertTrue(rulesSeen.containsAll(Set.of(
                        "HIGH_AMOUNT", "VELOCITY", "COUNTRY_CHANGE", "BLACKLIST", "NEW_DEVICE", "CARD_TESTING")),
                "every rule should appear in the demo data, but only these did: " + rulesSeen);

        MvcResult alerts = mvc.perform(get("/api/v1/alerts?status=OPEN&size=100").header("Authorization", auth))
                .andExpect(status().isOk())
                .andReturn();
        long open = json.readTree(alerts.getResponse().getContentAsString()).get("totalElements").asLong();
        assertTrue(open >= 5, "expected several open alerts but got " + open);
    }
}
