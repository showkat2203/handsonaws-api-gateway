package com.plm.service.domain;

import java.util.List;

/** Payload for creating or updating a {@link ChangeOrder}'s editable fields (title/description/affected parts). */
public record NewChangeOrder(String title, String description, List<String> affectedPartIds) {
}
