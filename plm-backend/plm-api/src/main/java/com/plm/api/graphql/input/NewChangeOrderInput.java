package com.plm.api.graphql.input;

import java.util.List;

public record NewChangeOrderInput(String title, String description, List<String> affectedPartIds) {
}
