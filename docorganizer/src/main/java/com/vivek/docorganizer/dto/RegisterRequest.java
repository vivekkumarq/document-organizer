package com.vivek.docorganizer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

    @NotBlank
    @Size(max = 100)
    @Schema(example = "Vivek Kumar")
    private String name;

    @Email
    @NotBlank
    @Size(max = 255)
    @Schema(example = "vivek@example.com")
    private String email;

    @NotBlank
    @Size(min = 8, max = 100, message = "must be between 8 and 100 characters")
    @Schema(example = "correct-horse-battery")
    private String password;

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }

    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }

    public void setPassword(String password) { this.password = password; }
}
