package dev.sentinel.api;

import dev.sentinel.engine.BlacklistKind;
import dev.sentinel.engine.Decision;
import dev.sentinel.persistence.AlertStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Request and response shapes of the HTTP API. Kept apart from the database entities on purpose. */
public final class ApiModels {

    private ApiModels() {}

    /** Amounts must be positive with at most two decimal places (see ADR-003). */
    public record IngestRequest(
            @NotBlank @Size(max = 64) String externalId,
            @NotBlank @Size(max = 64) String accountId,
            @NotBlank @Size(max = 64) String merchantId,
            @NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount,
            @NotBlank @Pattern(regexp = "[A-Z]{3}", message = "must be a 3-letter ISO currency code") String currency,
            @NotBlank @Pattern(regexp = "[A-Z]{2}", message = "must be a 2-letter ISO country code") String country,
            @Size(max = 64) String deviceId,
            @NotNull Instant occurredAt) {}

    public record ReasonView(String ruleId, int score, String reason) {}

    public record TransactionView(
            UUID id,
            String externalId,
            String accountId,
            String merchantId,
            BigDecimal amount,
            String currency,
            String country,
            Decision decision,
            int riskScore,
            Instant occurredAt,
            Instant receivedAt,
            List<ReasonView> reasons) {}

    public record AlertView(
            long id,
            AlertStatus status,
            Instant createdAt,
            String resolvedBy,
            Instant resolvedAt,
            String note,
            TransactionView transaction) {}

    public record ResolveRequest(@NotNull AlertStatus status, @Size(max = 500) String note) {}

    public record BlacklistRequest(
            @NotNull BlacklistKind kind,
            @NotBlank @Size(max = 64) String value,
            @Size(max = 200) String reason) {}

    public record BlacklistView(long id, BlacklistKind kind, String value, String reason,
                                String createdBy, Instant createdAt) {}

    public record LoginRequest(@NotBlank @Size(max = 64) String username, @NotBlank @Size(max = 200) String password) {}

    public record TokenResponse(String accessToken, String tokenType, long expiresInSeconds, String role) {}

    public record PageView<T>(List<T> items, int page, int size, long totalElements, int totalPages) {}
}
