package com.plm.service.security;

/**
 * Thread-local holder for the current {@link PlmPrincipal}. Both the GraphQL API and the
 * chatbot service populate this from the validated JWT at the start of a request/tool
 * invocation, and the service layer reads it to enforce authorization uniformly regardless
 * of which entry point is calling.
 */
public final class PlmPrincipalContext {

    private static final ThreadLocal<PlmPrincipal> CURRENT = ThreadLocal.withInitial(() -> PlmPrincipal.ANONYMOUS);

    private PlmPrincipalContext() {
    }

    public static PlmPrincipal get() {
        return CURRENT.get();
    }

    public static void set(PlmPrincipal principal) {
        CURRENT.set(principal == null ? PlmPrincipal.ANONYMOUS : principal);
    }

    public static void clear() {
        CURRENT.remove();
    }
}
