package com.plm.api.security;

import com.plm.service.security.Role;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Hardcoded demo identity store so the sample app is usable out of the box without standing up
 * a real identity provider. Swap for a real user store (Cognito, an RDBMS, etc.) in production —
 * everything downstream (JwtUtil, JwtAuthFilter, the service layer's Authz checks) is unaffected
 * since they only depend on the issued JWT's subject + roles claims.
 */
@Component
public class DemoUserStore {

    public record DemoUser(String username, String passwordHash, Set<Role> roles) {
    }

    private final Map<String, DemoUser> users;

    public DemoUserStore(PasswordEncoder encoder) {
        this.users = Map.of(
                "admin", new DemoUser("admin", encoder.encode("admin123"), Set.of(Role.ADMIN, Role.ENGINEER, Role.VIEWER)),
                "engineer", new DemoUser("engineer", encoder.encode("engineer123"), Set.of(Role.ENGINEER, Role.VIEWER)),
                "viewer", new DemoUser("viewer", encoder.encode("viewer123"), Set.of(Role.VIEWER))
        );
    }

    public Optional<DemoUser> find(String username) {
        return Optional.ofNullable(users.get(username));
    }
}
