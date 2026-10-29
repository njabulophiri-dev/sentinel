package dev.sentinel.service;

import dev.sentinel.api.ApiModels.LoginRequest;
import dev.sentinel.api.ApiModels.TokenResponse;
import dev.sentinel.config.SecurityProperties;
import dev.sentinel.persistence.UserEntity;
import dev.sentinel.persistence.UserRepository;
import java.time.Clock;
import java.time.Instant;
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

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtEncoder jwtEncoder;
    private final SecurityProperties properties;
    private final AuditService audit;
    private final Clock clock;
    /** Compared against when the username does not exist, so "no such user" takes as long as "wrong password". */
    private final String dummyHash;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtEncoder jwtEncoder,
                       SecurityProperties properties, AuditService audit, Clock clock) {
        this.users = users;
        this.encoder = encoder;
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
        this.audit = audit;
        this.clock = clock;
        this.dummyHash = encoder.encode("not-a-real-password");
    }

    public TokenResponse login(LoginRequest request) {
        UserEntity user = users.findByUsername(request.username()).orElse(null);
        boolean passwordMatches = encoder.matches(request.password(), user != null ? user.getPasswordHash() : dummyHash);
        if (user == null || !user.isEnabled() || !passwordMatches) {
            audit.record(request.username(), "LOGIN_FAILED", "user:" + request.username(), null);
            throw new BadCredentialsException("Invalid username or password");
        }

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
