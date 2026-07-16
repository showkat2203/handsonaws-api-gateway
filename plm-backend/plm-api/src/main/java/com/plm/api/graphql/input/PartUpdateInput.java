package com.plm.api.graphql.input;

import java.util.List;

public record PartUpdateInput(String name, String description, String revision, String supplierId,
                               List<KeyValueInput> attributes) {
}
