package com.vivek.docorganizer.dto.response;

import com.vivek.docorganizer.entity.User;

import java.time.LocalDateTime;

/** Public view of a user. Deliberately never carries the password hash. */
public record UserResponse(Long id, String name, String email, String role, LocalDateTime createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt());
    }
}
