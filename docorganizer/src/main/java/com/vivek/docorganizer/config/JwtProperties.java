package com.vivek.docorganizer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT signing configuration.
 *
 * <pre>
 * app.jwt.secret         -&gt; JWT_SECRET
 * app.jwt.expiration-ms  -&gt; JWT_EXPIRATION_MS
 * </pre>
 *
 * When {@code secret} is left blank a random HS256 key is generated at startup, which means
 * tokens do not survive a restart. That is fine for local development and deliberately
 * unusable in production without setting JWT_SECRET.
 */
@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    /** HMAC signing secret. Must be at least 32 characters when supplied. */
    private String secret = "";

    /** Token lifetime in milliseconds. Defaults to 24 hours. */
    private long expirationMs = 86_400_000L;

    public String getSecret() { return secret; }

    public void setSecret(String secret) { this.secret = secret; }

    public long getExpirationMs() { return expirationMs; }

    public void setExpirationMs(long expirationMs) { this.expirationMs = expirationMs; }
}
