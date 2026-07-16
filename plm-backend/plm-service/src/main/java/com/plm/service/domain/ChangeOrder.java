package com.plm.service.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** An Engineering Change Order (ECO) affecting one or more parts. */
public final class ChangeOrder {

    private final String id;
    private final String title;
    private final String description;
    private final ChangeOrderStatus status;
    private final List<String> affectedPartIds;
    private final String createdBy;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final Instant implementedAt;

    public ChangeOrder(String id, String title, String description, ChangeOrderStatus status,
                        List<String> affectedPartIds, String createdBy,
                        Instant createdAt, Instant updatedAt, Instant implementedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.title = Objects.requireNonNull(title, "title");
        this.description = description;
        this.status = Objects.requireNonNull(status, "status");
        this.affectedPartIds = affectedPartIds == null ? List.of() : List.copyOf(affectedPartIds);
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.implementedAt = implementedAt;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public ChangeOrderStatus getStatus() {
        return status;
    }

    public List<String> getAffectedPartIds() {
        return affectedPartIds;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getImplementedAt() {
        return implementedAt;
    }

    public ChangeOrder withStatus(ChangeOrderStatus newStatus, Instant updated, Instant implemented) {
        return new ChangeOrder(id, title, description, newStatus, affectedPartIds, createdBy,
                createdAt, updated, implemented);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChangeOrder that)) return false;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ChangeOrder{id='%s', title='%s', status=%s}".formatted(id, title, status);
    }
}
