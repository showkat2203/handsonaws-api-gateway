package com.plm.service.domain;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/** A hardware component: a rack, server, PSU, NIC, cable, etc. */
public final class Part {

    private final String id;
    private final String name;
    private final String description;
    private final PartType type;
    private final LifecycleState lifecycleState;
    private final String revision;
    private final String supplierId;
    private final Map<String, String> attributes;
    private final Instant createdAt;
    private final Instant updatedAt;

    public Part(String id, String name, String description, PartType type, LifecycleState lifecycleState,
                String revision, String supplierId, Map<String, String> attributes,
                Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.description = description;
        this.type = Objects.requireNonNull(type, "type");
        this.lifecycleState = Objects.requireNonNull(lifecycleState, "lifecycleState");
        this.revision = revision;
        this.supplierId = supplierId;
        this.attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public PartType getType() {
        return type;
    }

    public LifecycleState getLifecycleState() {
        return lifecycleState;
    }

    public String getRevision() {
        return revision;
    }

    public String getSupplierId() {
        return supplierId;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Part withLifecycleState(LifecycleState newState, Instant updated) {
        return new Part(id, name, description, type, newState, revision, supplierId, attributes, createdAt, updated);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Part part)) return false;
        return id.equals(part.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Part{id='%s', name='%s', type=%s, lifecycleState=%s, revision='%s'}"
                .formatted(id, name, type, lifecycleState, revision);
    }
}
