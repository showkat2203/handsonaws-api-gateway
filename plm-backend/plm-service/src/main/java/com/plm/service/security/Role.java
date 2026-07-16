package com.plm.service.security;

/**
 * Application roles. VIEWER is read-only, ENGINEER can create/update parts, BOM links, and change
 * orders, ADMIN can additionally approve/reject change orders and manage suppliers.
 */
public enum Role {
    VIEWER,
    ENGINEER,
    ADMIN
}
