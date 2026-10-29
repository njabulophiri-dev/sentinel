package dev.sentinel.api;

import dev.sentinel.api.ApiModels.IngestRequest;
import dev.sentinel.api.ApiModels.PageView;
import dev.sentinel.api.ApiModels.TransactionView;
import dev.sentinel.service.ApiErrors;
import dev.sentinel.service.IngestionService;
import dev.sentinel.service.TransactionQueryService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transactions")
class TransactionController {

    private final IngestionService ingestion;
    private final TransactionQueryService queries;

    TransactionController(IngestionService ingestion, TransactionQueryService queries) {
        this.ingestion = ingestion;
        this.queries = queries;
    }

    /**
     * Assess a transaction. Retrying with the same Idempotency-Key and the same body returns the
     * original result (200, Idempotent-Replay: true) instead of creating a second transaction.
     */
    @PostMapping
    ResponseEntity<TransactionView> ingest(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody IngestRequest request) {
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 100) {
            throw new ApiErrors.BadRequest("The Idempotency-Key header is required (at most 100 characters)");
        }
        IngestionService.Result result = ingestion.ingest(idempotencyKey.trim(), request);
        if (result.replayed()) {
            return ResponseEntity.ok().header("Idempotent-Replay", "true").body(result.transaction());
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(result.transaction());
    }

    @GetMapping("/{id}")
    TransactionView get(@PathVariable UUID id) {
        return queries.get(id);
    }

    @GetMapping
    PageView<TransactionView> list(@RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "20") int size) {
        return queries.list(page, size);
    }
}
