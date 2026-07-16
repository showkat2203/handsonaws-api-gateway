package com.plm.service.domain;

import java.util.Map;

/** Payload for creating a new {@link Part}. Starts life in {@code DESIGN} lifecycle state. */
public record NewPart(String name, String description, PartType type, String revision,
                       String supplierId, Map<String, String> attributes) {
}
