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
    private final LoginAttemptLimiter ipLimiter;
    private final LoginAttemptLimiter userLimiter;
    /** Compared against when the username does not exist, so "no such user" takes as long as "wrong password". */
    private final String dummyHash;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtEncoder jwtEncoder,
                       SecurityProperties properties, LoginThrottleProperties throttle,
                       AuditService audit, Clock clock) {
        this.users = users;
        this.encoder = encoder;
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
        this.audit = audit;
        this.clock = clock;
        this.ipLimiter = new LoginAttemptLimiter(throttle.maxFailuresPerIp(), throttle.window(), clock);
        this.userLimiter = new LoginAttemptLimiter(throttle.maxFailuresPerUser(), throttle.window(), clock);
        this.dummyHash = encoder.encode("not-a-real-password");
    }

    public TokenResponse login(LoginRequest request, String clientIp) {
        String ipKey = clientIp;
        String userKey = clientIp + "|" + request.username().toLowerCase(Locale.ROOT);

        // Refuse before doing any expensive password hashing, so guessing cannot also be used to burn CPU.
        if (ipLimiter.isBlocked(ipKey) || userLimiter.isBlocked(userKey)) {
            Duration ipWait = ipLimiter.retryAfter(ipKey);
            Duration userWait = userLimiter.retryAfter(userKey);
            Duration wait = ipWait.compareTo(userWait) >= 0 ? ipWait : userWait;
            log.warn("Sign-in throttled for {}", clientIp); // logged, not audited, so an attacker cannot fill the audit table
            throw new ApiErrors.TooManyRequests("Too many failed sign-in attempts. Try again later.",
                    Math.max(1, wait.toSeconds()));
        }

        UserEntity user = users.findByUsername(request.username()).orElse(null);
        boolean passwordMatches = encoder.matches(request.password(), user != null ? user.getPasswordHash() : dummyHash);
        if (user == null || !user.isEnabled() || !passwordMatches) {
            ipLimiter.recordFailure(ipKey);
            userLimiter.recordFailure(userKey);
            audit.record(request.username(), "LOGIN_FAILED", "user:" + request.username(), null);
            throw new BadCredentialsException("Invalid username or password");
        }

        userLimiter.reset(userKey);
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
