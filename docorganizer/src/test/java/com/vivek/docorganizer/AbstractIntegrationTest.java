package com.vivek.docorganizer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vivek.docorganizer.repository.DocumentRepository;
import com.vivek.docorganizer.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Shared harness for the MockMvc integration tests: H2 via the {@code test} profile, a clean
 * database before every test, and a helper that registers a user and returns a usable token.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    protected static final String PASSWORD = "sup3r-secret-pw";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected DocumentRepository documentRepository;

    @BeforeEach
    void resetDatabase() {
        documentRepository.deleteAll();
        userRepository.deleteAll();
    }

    protected void register(String name, String email, String password) throws Exception {

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", name, "email", email, "password", password))))
                .andExpect(status().isCreated());
    }

    protected String login(String email, String password) throws Exception {

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());

        return body.get("token").asText();
    }

    /** Registers an account and returns the {@code Bearer ...} header value for it. */
    protected String registerAndAuthorize(String email) throws Exception {

        register("Test " + email, email, PASSWORD);

        return "Bearer " + login(email, PASSWORD);
    }
}
