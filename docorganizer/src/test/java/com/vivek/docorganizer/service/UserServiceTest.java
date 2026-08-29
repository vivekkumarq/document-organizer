package com.vivek.docorganizer.service;

import com.vivek.docorganizer.dto.RegisterRequest;
import com.vivek.docorganizer.entity.User;
import com.vivek.docorganizer.exception.ConflictException;
import com.vivek.docorganizer.exception.InvalidCredentialsException;
import com.vivek.docorganizer.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Unit tests for registration and login, backed by an in-memory stand-in repository. */
class UserServiceTest {

    private final Map<String, User> store = new HashMap<>();

    private UserService userService;

    @BeforeEach
    void setUp() {

        store.clear();

        UserRepository repository = mock(UserRepository.class);

        when(repository.existsByEmail(anyString()))
                .thenAnswer(invocation -> store.containsKey(invocation.getArgument(0)));

        when(repository.findByEmail(anyString()))
                .thenAnswer(invocation -> Optional.ofNullable(store.get(invocation.getArgument(0))));

        when(repository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            store.put(user.getEmail(), user);
            return user;
        });

        userService = new UserService(repository, new BCryptPasswordEncoder());
    }

    @Test
    @DisplayName("register hashes the password and normalises the email to lowercase")
    void registerHashesAndNormalises() {

        User user = userService.registerUser(request("Vivek", "  VIVEK@Example.COM ", "hunter2hunter2"));

        assertThat(user.getEmail()).isEqualTo("vivek@example.com");
        assertThat(user.getRole()).isEqualTo("USER");
        assertThat(user.getPassword()).isNotEqualTo("hunter2hunter2").startsWith("$2");
    }

    @Test
    @DisplayName("register refuses an email that is already taken")
    void registerRejectsDuplicates() {

        userService.registerUser(request("Vivek", "vivek@example.com", "hunter2hunter2"));

        assertThatThrownBy(() ->
                userService.registerUser(request("Other", "VIVEK@example.com", "hunter2hunter2")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("login accepts the right password and rejects a wrong one")
    void loginChecksPassword() {

        userService.registerUser(request("Vivek", "vivek@example.com", "hunter2hunter2"));

        assertThat(userService.login("vivek@example.com", "hunter2hunter2").getName()).isEqualTo("Vivek");

        assertThatThrownBy(() -> userService.login("vivek@example.com", "wrong"))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    @DisplayName("login on an unknown account fails with the same message as a wrong password")
    void loginDoesNotLeakAccountExistence() {

        assertThatThrownBy(() -> userService.login("nobody@example.com", "whatever"))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");
    }

    private static RegisterRequest request(String name, String email, String password) {

        RegisterRequest request = new RegisterRequest();
        request.setName(name);
        request.setEmail(email);
        request.setPassword(password);

        return request;
    }
}
