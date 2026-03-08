package com.vivek.docorganizer.controller;

import com.vivek.docorganizer.dto.RegisterRequest;
import com.vivek.docorganizer.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import com.vivek.docorganizer.dto.LoginRequest;
import com.vivek.docorganizer.security.JwtUtil;


@RestController
@RequestMapping("/auth")
public class AuthController {
    private final JwtUtil jwtUtil;
    private final UserService userService;

    public AuthController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public String register(@Valid @RequestBody RegisterRequest request) {

        userService.registerUser(request);

        return "User registered successfully";
    }

    @PostMapping("/login")
    public String login(@RequestBody LoginRequest request) {

        var user = userService.login(request.getEmail(), request.getPassword());

        return jwtUtil.generateToken(user.getEmail());
    }
}