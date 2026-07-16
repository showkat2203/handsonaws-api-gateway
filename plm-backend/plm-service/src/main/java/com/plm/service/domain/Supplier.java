package com.plm.service.domain;

import java.util.Objects;

/** A supplier / vendor of one or more parts. */
public final class Supplier {

    private final String id;
    private final String name;
    private final String contactEmail;
    private final String address;
    private final boolean active;

    public Supplier(String id, String name, String contactEmail, String address, boolean active) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.contactEmail = contactEmail;
        this.address = address;
        this.active = active;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public String getAddress() {
        return address;
    }

    public boolean isActive() {
        return active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Supplier supplier)) return false;
        return id.equals(supplier.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Supplier{id='%s', name='%s', active=%s}".formatted(id, name, active);
    }
}
