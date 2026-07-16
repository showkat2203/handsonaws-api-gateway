package com.plm.service.security;

import com.plm.service.exception.AuthorizationException;

/** Small helper for the service layer to enforce role requirements against the current principal. */
public final class Authz {

    private Authz() {
    }

    public static void require(Role minimumRole) {
        PlmPrincipal principal = PlmPrincipalContext.get();
        if (!principal.hasAtLeast(minimumRole)) {
            throw new AuthorizationException(
                    "User '%s' requires role >= %s".formatted(principal.getUsername(), minimumRole));
        }
    }
}
