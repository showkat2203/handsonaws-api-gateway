package com.plm.api.graphql.input;

import com.plm.service.domain.LifecycleState;
import com.plm.service.domain.PartType;

public record PartFilterInput(String nameContains, PartType type, LifecycleState lifecycleState,
                               String supplierId, Integer page, Integer size) {
}
