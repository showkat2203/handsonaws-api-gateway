package com.plm.service.domain;

import java.util.Map;

/** Payload for updating mutable fields of an existing {@link Part}. Null fields are left unchanged. */
public record PartUpdate(String name, String description, String revision, String supplierId,
                          Map<String, String> attributes) {
}
