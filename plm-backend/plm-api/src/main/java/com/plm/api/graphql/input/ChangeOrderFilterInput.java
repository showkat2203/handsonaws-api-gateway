package com.plm.api.graphql.input;

import com.plm.service.domain.ChangeOrderStatus;

public record ChangeOrderFilterInput(ChangeOrderStatus status, String partId, Integer page, Integer size) {
}
