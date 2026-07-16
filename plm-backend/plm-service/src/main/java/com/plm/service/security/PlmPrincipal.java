package com.plm.service.security;

import java.util.Objects;
import java.util.Set;

/**
 * The authenticated identity a request is running as. Populated from the JWT by both the
 * GraphQL API and the chatbot service's auth filters, and threaded through the internal
 * service layer so both entry points enforce identical authorization.
 */
public final class PlmPrincipal {

    public static final PlmPrincipal ANONYMOUS = new PlmPrincipal("anonymous", Set.of());

    private final String username;
    private final Set<Role> roles;

    public PlmPrincipal(String username, Set<Role> roles) {
        this.username = Objects.requireNonNull(username, "username");
        this.roles = roles == null ? Set.of() : Set.copyOf(roles);
    }

    public String getUsername() {
        return username;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public boolean hasRole(Role role) {
        return roles.contains(role);
    }

    public boolean hasAtLeast(Role minimum) {
        return roles.stream().anyMatch(r -> r.ordinal() >= minimum.ordinal());
    }
}
