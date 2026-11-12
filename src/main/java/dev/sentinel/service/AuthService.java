package dev.sentinel.service;

import dev.sentinel.api.ApiModels.LoginRequest;
import dev.sentinel.api.ApiModels.TokenResponse;
import dev.sentinel.config.LoginThrottleProperties;
import dev.sentinel.config.SecurityProperties;
import dev.sentinel.persistence.UserEntity;
import dev.sentinel.persistence.UserRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtEncoder jwtEncoder;
    private final SecurityProperties properties;
    private final AuditService audit;
    private final Clock clock;
    private final LoginThrottleProperties throttle;
    private final LoginAttemptStore attempts;
    /** Compared against when the username does not exist, so "no such user" takes as long as "wrong password". */
    private final String dummyHash;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtEncoder jwtEncoder,
                       SecurityProperties properties, LoginThrottleProperties throttle,
                       AuditService audit, Clock clock, LoginAttemptStore attempts) {
        this.users = users;
        this.encoder = encoder;
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
        this.audit = audit;
        this.clock = clock;
        this.attempts = attempts;
        this.throttle = throttle;
        this.dummyHash = encoder.encode("not-a-real-password");
    }

    public TokenResponse login(LoginRequest request, String clientIp) {
        String normalizedUsername = request.username().toLowerCase(Locale.ROOT);
        String ipKey = "ip|" + clientIp;
        String userKey = "source-user|" + clientIp + "|" + normalizedUsername;
        String accountKey = "account|" + normalizedUsername;

        boolean ipBlocked = attempts.isBlocked(ipKey, throttle.maxFailuresPerIp(), throttle.window());
        boolean userBlocked = attempts.isBlocked(userKey, throttle.maxFailuresPerUser(), throttle.window());
        if (ipBlocked || userBlocked) {
            Duration ipWait = ipBlocked ? attempts.retryAfter(ipKey) : Duration.ZERO;
            Duration userWait = userBlocked ? attempts.retryAfter(userKey) : Duration.ZERO;
            Duration wait = ipWait.compareTo(userWait) >= 0 ? ipWait : userWait;
            log.warn("Sign-in throttled for {}", clientIp); // logged, not audited, so an attacker cannot fill the audit table
            throw new ApiErrors.TooManyRequests("Too many failed sign-in attempts. Try again later.",
                    Math.max(1, wait.toSeconds()));
        }

        @Nullable UserEntity user = users.findByUsername(request.username()).orElse(null);
        boolean passwordMatches = encoder.matches(request.password(), user != null ? user.getPasswordHash() : dummyHash);
        if (user == null || !user.isEnabled() || !passwordMatches) {
            attempts.recordFailure(ipKey, throttle.window());
            attempts.recordFailure(userKey, throttle.window());
            attempts.recordFailure(accountKey, throttle.window());
            if (attempts.isBlocked(accountKey, throttle.maxFailuresPerAccount(), throttle.window())) {
                Duration wait = attempts.retryAfter(accountKey);
                log.warn("Account sign-in attempts throttled after repeated invalid credentials");
                throw new ApiErrors.TooManyRequests("Too many failed sign-in attempts for this account.",
                        Math.max(1, wait.toSeconds()));
            }
            audit.record(request.username(), "LOGIN_FAILED", "user:" + request.username(), null);
            throw new BadCredentialsException("Invalid username or password");
        }

        attempts.reset(userKey);
        attempts.reset(accountKey);
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("sentinel")
                .subject(user.getUsername())
                .issuedAt(now)
                .expiresAt(now.plus(properties.tokenTtl()))
                .claim("role", user.getRole().name())
                .build();
        String token = jwtEncoder.encode(
                JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        audit.record(user.getUsername(), "LOGIN_OK", "user:" + user.getUsername(), null);
        return new TokenResponse(token, "Bearer", properties.tokenTtl().toSeconds(), user.getRole().name());
    }
}
