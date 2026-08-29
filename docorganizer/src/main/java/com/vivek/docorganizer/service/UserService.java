package com.vivek.docorganizer.service;

import com.vivek.docorganizer.dto.RegisterRequest;
import com.vivek.docorganizer.entity.User;
import com.vivek.docorganizer.exception.ConflictException;
import com.vivek.docorganizer.exception.InvalidCredentialsException;
import com.vivek.docorganizer.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
public class UserService {

    /** Same message for an unknown account and a wrong password, to avoid user enumeration. */
    private static final String BAD_CREDENTIALS = "Invalid email or password";

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User registerUser(RegisterRequest request) {

        String email = normaliseEmail(request.getEmail());

        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email already registered");
        }

        User user = new User(
                request.getName().trim(),
                email,
                passwordEncoder.encode(request.getPassword()),
                "USER",
                LocalDateTime.now());

        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User login(String email, String password) {

        User user = userRepository.findByEmail(normaliseEmail(email))
                .orElseThrow(() -> new InvalidCredentialsException(BAD_CREDENTIALS));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new InvalidCredentialsException(BAD_CREDENTIALS);
        }

        return user;
    }

    private static String normaliseEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
