package dev.sentinel.config;

import dev.sentinel.api.ApiModels.IngestRequest;
import dev.sentinel.engine.BlacklistKind;
import dev.sentinel.persistence.BlacklistEntity;
import dev.sentinel.persistence.BlacklistRepository;
import dev.sentinel.persistence.TransactionRepository;
import dev.sentinel.service.IngestionService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Fills an empty database with a small, realistic story so a visitor to a live demo sees every rule in action.
 * Runs only when sentinel.demo.seed=true, and only if there are no transactions yet.
 * Every transaction goes through the real ingestion path, so the decisions are produced by the real engine.
 */
@Component
@ConditionalOnProperty(name = "sentinel.demo.seed", havingValue = "true")
class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final TransactionRepository transactions;
    private final BlacklistRepository blacklist;
    private final IngestionService ingestion;
    private final Clock clock;
    private int sequence;

    DemoDataSeeder(TransactionRepository transactions, BlacklistRepository blacklist,
                   IngestionService ingestion, Clock clock) {
        this.transactions = transactions;
        this.blacklist = blacklist;
        this.ingestion = ingestion;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (transactions.count() > 0) {
            return;
        }
        Instant now = clock.instant();
        blacklist.save(new BlacklistEntity(BlacklistKind.MERCHANT, "MER-DARKSHOP",
                "Reported for chargeback fraud", "seed", now));

        // Ordinary purchases: approved, nothing to see.
        send("ACC-1001", "MER-GROCER", "149.99", "ZA", "DEV-1001", now.minus(Duration.ofHours(5)));
        send("ACC-1002", "MER-FUEL", "89.50", "ZA", "DEV-1002", now.minus(Duration.ofHours(4)));
        send("ACC-1003", "MER-ELECTRONICS", "1299.00", "ZA", "DEV-1003", now.minus(Duration.ofHours(3)));

        // One large payment: goes to review. One enormous payment: declined.
        send("ACC-2001", "MER-JEWELLER", "7500.00", "ZA", "DEV-2001", now.minus(Duration.ofMinutes(50)));
        send("ACC-2002", "MER-JEWELLER", "62000.00", "ZA", "DEV-2002", now.minus(Duration.ofMinutes(45)));

        // The same account in South Africa, then the United Kingdom 25 minutes later.
        send("ACC-3001", "MER-CAFE", "450.00", "ZA", "DEV-3001", now.minus(Duration.ofMinutes(40)));
        send("ACC-3001", "MER-ONLINE", "1250.00", "GB", "DEV-3001", now.minus(Duration.ofMinutes(15)));

        // The same two signals together: a large payment in a second country.
        send("ACC-3002", "MER-CAFE", "300.00", "ZA", "DEV-3002", now.minus(Duration.ofMinutes(50)));
        send("ACC-3002", "MER-ONLINE", "8200.00", "GB", "DEV-3002", now.minus(Duration.ofMinutes(10)));

        // Six quick payments: the sixth records a velocity hit (not enough on its own for review).
        for (int i = 0; i < 6; i++) {
            send("ACC-4001", "MER-TICKETS", "80.00", "ZA", "DEV-4001", now.minus(Duration.ofMinutes(6 - i)));
        }

        // A burst of tiny payments: the classic card-testing pattern.
        for (int i = 0; i < 5; i++) {
            send("ACC-5001", "MER-DONATE", "5.00", "ZA", "DEV-5001", now.minus(Duration.ofSeconds(240 - i * 45L)));
        }

        // A familiar device for a while, then a larger payment from a new one.
        send("ACC-6001", "MER-GROCER", "200.00", "ZA", "DEV-6001-A", now.minus(Duration.ofHours(3)));
        send("ACC-6001", "MER-GROCER", "350.00", "ZA", "DEV-6001-A", now.minus(Duration.ofHours(2)));
        send("ACC-6001", "MER-ELECTRONICS", "2500.00", "ZA", "DEV-6001-Z", now.minus(Duration.ofMinutes(5)));

        // A payment to a blacklisted merchant.
        send("ACC-7001", "MER-DARKSHOP", "899.00", "ZA", "DEV-7001", now.minus(Duration.ofMinutes(2)));

        log.info("Demo data loaded: {} transactions", transactions.count());
    }

    private void send(String account, String merchant, String amount, String country, String device, Instant at) {
        int n = ++sequence;
        String id = "%03d".formatted(n);
        ingestion.ingest("demo-seed-" + id, new IngestRequest(
                "DEMO-" + id, account, merchant, new BigDecimal(amount), "ZAR", country, device, at));
    }
}
