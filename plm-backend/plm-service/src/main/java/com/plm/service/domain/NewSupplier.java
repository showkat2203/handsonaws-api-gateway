package com.plm.service.domain;

/** Payload for creating or replacing a {@link Supplier}'s mutable fields. */
public record NewSupplier(String name, String contactEmail, String address, boolean active) {
}
