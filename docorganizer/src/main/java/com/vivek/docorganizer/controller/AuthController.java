package com.vivek.docorganizer.controller;

import com.vivek.docorganizer.dto.LoginRequest;
import com.vivek.docorganizer.dto.RegisterRequest;
import com.vivek.docorganizer.dto.response.AuthResponse;
import com.vivek.docorganizer.dto.response.UserResponse;
import com.vivek.docorganizer.entity.User;
import com.vivek.docorganizer.security.CurrentUser;
import com.vivek.docorganizer.security.JwtUtil;
import com.vivek.docorganizer.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Account registration and JWT issuance")
public class AuthController {

    private final JwtUtil jwtUtil;
    private final UserService userService;
    private final CurrentUser currentUser;

    public AuthController(UserService userService, JwtUtil jwtUtil, CurrentUser currentUser) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.currentUser = currentUser;
    }

    @PostMapping("/register")
    @SecurityRequirements
    @Operation(summary = "Register a new account",
            description = "Creates a user with a BCrypt-hashed password. Does not return a token; "
                    + "call /api/auth/login afterwards.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Account created"),
            @ApiResponse(responseCode = "400", description = "Validation failed", content = @io.swagger.v3.oas.annotations.media.Content),
            @ApiResponse(responseCode = "409", description = "Email already registered", content = @io.swagger.v3.oas.annotations.media.Content)
    })
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {

        User user = userService.registerUser(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Exchange credentials for a JWT")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authenticated"),
            @ApiResponse(responseCode = "401", description = "Invalid email or password", content = @io.swagger.v3.oas.annotations.media.Content)
    })
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {

        User user = userService.login(request.getEmail(), request.getPassword());

        String token = jwtUtil.generateToken(user.getEmail());

        return AuthResponse.of(token, jwtUtil.getExpirationMs(), UserResponse.from(user));
    }

    @GetMapping("/me")
    @Operation(summary = "Return the profile behind the supplied token")
    @ApiResponse(responseCode = "200", description = "Current user")
    public UserResponse me() {
        return UserResponse.from(currentUser.require());
    }
}
