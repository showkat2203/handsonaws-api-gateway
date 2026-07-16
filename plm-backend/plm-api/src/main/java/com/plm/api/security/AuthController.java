package com.plm.api.security;

import com.plm.service.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Demo login endpoint that issues the JWTs consumed by both the GraphQL API and the chatbot's
 * {@code /chat} endpoint. See {@link DemoUserStore} for the three seeded demo accounts.
 */
@RestController
public class AuthController {

    public record LoginRequest(String username, String password) {
    }

    public record LoginResponse(String token, String username, java.util.Set<String> roles) {
    }

    private final DemoUserStore userStore;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthController(DemoUserStore userStore, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userStore = userStore;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/auth/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        DemoUserStore.DemoUser user = userStore.find(request.username())
                .filter(u -> passwordEncoder.matches(request.password(), u.passwordHash()))
                .orElseThrow(() -> new org.springframework.security.authentication.BadCredentialsException("Invalid username or password"));

        String token = jwtUtil.issue(user.username(), user.roles());
        return new LoginResponse(token, user.username(),
                user.roles().stream().map(Enum::name).collect(java.util.stream.Collectors.toSet()));
    }
}
