package com.plm.api.graphql;

import com.plm.api.graphql.input.KeyValueInput;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Small conversions between plm-service domain types and GraphQL-friendly shapes. */
public final class GraphQLMappers {

    private GraphQLMappers() {
    }

    public record KeyValue(String key, String value) {
    }

    public static List<KeyValue> toKeyValues(Map<String, String> attributes) {
        return attributes.entrySet().stream()
                .map(e -> new KeyValue(e.getKey(), e.getValue()))
                .toList();
    }

    public static Map<String, String> toAttributeMap(List<KeyValueInput> input) {
        if (input == null) {
            return Map.of();
        }
        Map<String, String> map = new LinkedHashMap<>();
        for (KeyValueInput kv : input) {
            map.put(kv.key(), kv.value());
        }
        return map;
    }
}
