package com.vivek.docorganizer.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/** Returned by POST /api/auth/login. */
public record AuthResponse(
        @Schema(example = "eyJhbGciOiJIUzI1NiJ9...") String token,
        @Schema(example = "Bearer") String tokenType,
        @Schema(example = "86400000") long expiresInMs,
        UserResponse user) {

    public static AuthResponse of(String token, long expiresInMs, UserResponse user) {
        return new AuthResponse(token, "Bearer", expiresInMs, user);
    }
}
