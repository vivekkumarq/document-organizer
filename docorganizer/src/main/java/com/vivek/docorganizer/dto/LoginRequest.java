package com.vivek.docorganizer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public class LoginRequest {

    @NotBlank
    @Schema(example = "vivek@example.com")
    private String email;

    @NotBlank
    @Schema(example = "correct-horse-battery")
    private String password;

    public String getEmail() { return email; }

    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }

    public void setPassword(String password) { this.password = password; }
}
