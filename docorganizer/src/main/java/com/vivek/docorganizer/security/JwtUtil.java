package com.vivek.docorganizer.security;

import com.vivek.docorganizer.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

/**
 * Issues and verifies the HS256 tokens used for stateless authentication.
 *
 * <p>The signing key comes from {@code app.jwt.secret} (env {@code JWT_SECRET}). It is never
 * hardcoded: if no secret is configured a random key is generated for this JVM only.
 */
@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    /** Minimum secret length for HS256, in bytes, per RFC 7518. */
    private static final int MIN_SECRET_BYTES = 32;

    private final Key key;
    private final long expirationMs;

    public JwtUtil(JwtProperties properties) {

        String secret = properties.getSecret() == null ? "" : properties.getSecret().trim();

        if (secret.isEmpty()) {
            this.key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
            log.warn("app.jwt.secret is not set - generated an ephemeral signing key. "
                    + "Issued tokens will be rejected after a restart. Set JWT_SECRET in production.");
        } else if (secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "app.jwt.secret must be at least " + MIN_SECRET_BYTES + " characters for HS256");
        } else {
            this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        }

        this.expirationMs = properties.getExpirationMs();
    }

    public String generateToken(String email) {

        Date now = new Date();

        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expirationMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Verifies the signature and expiry of a token.
     *
     * @return the subject, which is the email of the authenticated user
     */
    public String extractEmail(String token) {

        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject();
    }

    public long getExpirationMs() {
        return expirationMs;
    }
}
