package com.vivek.docorganizer.controller;

import com.vivek.docorganizer.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("register creates the account and never echoes the password")
    void registerCreatesAccount() throws Exception {

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Vivek Kumar",
                                "email", "vivek@example.com",
                                "password", PASSWORD))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("vivek@example.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.password").doesNotExist());

        assertThat(userRepository.findByEmail("vivek@example.com")).isPresent();
    }

    @Test
    @DisplayName("stored password is a BCrypt hash, not the plaintext")
    void passwordIsHashed() throws Exception {

        register("Vivek", "hash@example.com", PASSWORD);

        String stored = userRepository.findByEmail("hash@example.com").orElseThrow().getPassword();

        assertThat(stored).isNotEqualTo(PASSWORD).startsWith("$2");
    }

    @Test
    @DisplayName("registering the same email twice returns 409")
    void duplicateEmailIsRejected() throws Exception {

        register("Vivek", "dupe@example.com", PASSWORD);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Someone Else",
                                "email", "dupe@example.com",
                                "password", PASSWORD))))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("register rejects an invalid email and a short password with 400")
    void registerValidation() throws Exception {

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Vivek",
                                "email", "not-an-email",
                                "password", PASSWORD))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Vivek",
                                "email", "short@example.com",
                                "password", "abc"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("login returns a JWT and the caller's profile")
    void loginReturnsToken() throws Exception {

        register("Vivek", "login@example.com", PASSWORD);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "login@example.com",
                                "password", PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value("login@example.com"));
    }

    @Test
    @DisplayName("a wrong password returns 401, not 400")
    void wrongPasswordIsUnauthorized() throws Exception {

        register("Vivek", "wrong@example.com", PASSWORD);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "wrong@example.com",
                                "password", "definitely-not-it"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    @DisplayName("an unknown account gets the same 401 message as a wrong password")
    void unknownAccountDoesNotLeakExistence() throws Exception {

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "ghost@example.com",
                                "password", PASSWORD))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    @DisplayName("/me needs a token and returns the token holder")
    void meRequiresToken() throws Exception {

        String auth = registerAndAuthorize("me@example.com");

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/auth/me").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("me@example.com"));
    }

    @Test
    @DisplayName("a garbage or tampered token is rejected with 401")
    void invalidTokenIsRejected() throws Exception {

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized());

        String auth = registerAndAuthorize("tamper@example.com");

        // Flip the last character of the signature.
        String tampered = auth.substring(0, auth.length() - 1)
                + (auth.endsWith("A") ? "B" : "A");

        mockMvc.perform(get("/api/auth/me").header("Authorization", tampered))
                .andExpect(status().isUnauthorized());
    }
}
