package com.vivek.docorganizer.security;

import com.vivek.docorganizer.entity.User;
import com.vivek.docorganizer.exception.NotFoundException;
import com.vivek.docorganizer.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Single place that resolves the authenticated principal to a {@link User} row.
 *
 * <p>Every owner-scoped operation goes through here rather than trusting a user id supplied by
 * the client, which is what previously allowed one account to read another account's documents.
 */
@Component
public class CurrentUser {

    private final UserRepository userRepository;

    public CurrentUser(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public String email() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new NotFoundException("No authenticated user");
        }

        return String.valueOf(authentication.getPrincipal());
    }

    public User require() {

        String email = email();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Authenticated user no longer exists"));
    }
}
