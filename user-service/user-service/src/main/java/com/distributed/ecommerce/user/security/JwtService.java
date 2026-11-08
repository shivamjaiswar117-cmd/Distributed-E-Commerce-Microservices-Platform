package com.distributed.ecommerce.user.security;

import com.distributed.ecommerce.user.entity.User;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Service
public class JwtService {

    private final JwtEncoder encoder;
    private final long expirySeconds;

    public JwtService(
            @Value("${app.jwt.secret:dev-only-secret-change-me-0123456789abcdef}") String secret,
            @Value("${app.jwt.expiry-seconds:86400}") long expirySeconds) {
        byte[] key = secret.getBytes(StandardCharsets.UTF_8);
        if (key.length < 32) {
            throw new IllegalStateException("app.jwt.secret must be at least 32 bytes");
        }
        this.encoder = new NimbusJwtEncoder(
                new ImmutableSecret<>(new SecretKeySpec(key, "HmacSHA256")));
        this.expirySeconds = expirySeconds;
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(user.getEmail())
                .claim("userId", user.getId())
                .claim("name", user.getFullName())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(expirySeconds))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}