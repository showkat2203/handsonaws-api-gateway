package com.plm.api.graphql.input;

import com.plm.service.domain.PartType;

import java.util.List;

public record NewPartInput(String name, String description, PartType type, String revision,
                            String supplierId, List<KeyValueInput> attributes) {
}
